package io.vyne.models.expressions

import com.winterbe.expekt.should
import io.vyne.models.EvaluatedExpression
import io.vyne.models.FailedEvaluatedExpression
import io.vyne.models.OperationResult
import io.vyne.models.Provided
import io.vyne.models.TypeNamedInstance
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.models.TypedObject
import io.vyne.models.TypedValue
import io.vyne.models.functions.FunctionRegistry
import io.vyne.models.functions.functionOf
import io.vyne.models.functions.stdlib.withoutWhitespace
import io.vyne.models.json.parseJson
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.testVyne
import io.vyne.typedObjects
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ExpressionTest {

   @Test
   fun `can discover inputs from services`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
            type Symbol inherits String
            type Quantity inherits Decimal
            type Price inherits Decimal
            type OrderCost inherits Decimal by Quantity * Price
            model Order {
               symbol : Symbol
               quantity : Quantity
            }
            model PricedOrder {
               symbol : Symbol
               quantity : Quantity
               cost : OrderCost
            }
            model SymbolPrice {
               symbol : Symbol
               price : Price
            }
            service PricingService {
               operation getPrice(Symbol):SymbolPrice
            }
         """.trimIndent()
      )
      val symbolPrice = vyne.parseJson("SymbolPrice", """{ "symbol" : "GBPNZD" , "price" : 0.48 }""")
      stub.addResponse("getPrice", listOf(symbolPrice), modifyDataSource = true)

      val order = vyne.parseJson("Order", """{ "symbol" : "GBPNZD" , "quantity" :  100 }""")

      // Exploring -- not really sure the best API here to ask Vyne to "hydrate"
      // an object with expressions.
      // Here, we're doing hydration on projection.
      val builtQuote = vyne.from(order).build("PricedOrder")
         .typedObjects()
         .first()
      builtQuote.toRawObject().should.equal(
         mapOf(
            "symbol" to "GBPNZD",
            "quantity" to 100.toBigDecimal(),
            "cost" to 48.00.toBigDecimal().setScale(2)
         )
      )
      val expressionSource = builtQuote["cost"].source as EvaluatedExpression
      expressionSource.inputs[0].value.should.equal(100.toBigDecimal())
      expressionSource.inputs[1].value.should.equal(0.48.toBigDecimal())
      val inputFromRemoteServiceDataSource = expressionSource.inputs[1].source as OperationResult
      ((inputFromRemoteServiceDataSource.inputs[0].value) as TypeNamedInstance).value.should.equal("GBPNZD")
   }

   @Test
   fun `when projecting a collection containing lookups, then the correct values from each model is used for resolution`(): Unit =
      runBlocking {
         val (vyne, stub) = testVyne(
            """
            type Symbol inherits String
            type Quantity inherits Decimal
            type Price inherits Decimal
            type OrderCost inherits Decimal by Quantity * Price
            model Order {
               symbol : Symbol
               quantity : Quantity
            }
            model PricedOrder {
               symbol : Symbol
               quantity : Quantity
               cost : OrderCost
            }
            model SymbolPrice {
               symbol : Symbol
               price : Price
            }
            service PricingService {
               operation getPrice(Symbol):SymbolPrice
            }
         """.trimIndent()
         )
         val symbolPrice = vyne.parseJson("SymbolPrice", """{ "symbol" : "GBPNZD" , "price" : 0.48 }""")
         stub.addResponse("getPrice", modifyDataSource = true) { _, params ->
            val symbol = params.first().second.value as String
            val prices = mapOf(
               "GBPNZD" to 0.48.toBigDecimal(),
               "AUDNZD" to 1.1.toBigDecimal()
            )
            val price = prices[symbol]!!
            val symbolPrice = vyne.parseJson("SymbolPrice", """{ "symbol" : "$symbol" , "price" : $price }""")
            listOf(symbolPrice)
         }

         val orders = vyne.parseJson(
            "Order[]", """[
         |{ "symbol" : "GBPNZD" , "quantity" :  100 },
         |{ "symbol" : "AUDNZD" , "quantity" :  50 }
         |]""".trimMargin()
         )

         // Exploring -- not really sure the best API here to ask Vyne to "hydrate"
         // an object with expressions.
         // Here, we're doing hydration on projection.
         val builtQuote = vyne.from(orders).build("PricedOrder[]")
            .typedObjects()
         val gbpNzdSource = builtQuote.first { it["symbol"]!!.value == "GBPNZD" }
            .get("cost")
            .source as EvaluatedExpression
         val gbpNzdOperationResultSource = gbpNzdSource.inputs[1].source as OperationResult
         ((gbpNzdOperationResultSource.inputs[0].value) as TypeNamedInstance).value.should.equal("GBPNZD")

         val audNzdSource = builtQuote.first { it["symbol"]!!.value == "AUDNZD" }
            .get("cost")
            .source as EvaluatedExpression
         val operationResultSource = audNzdSource.inputs[1].source as OperationResult
         ((operationResultSource.inputs[0].value) as TypeNamedInstance).value.should.equal("AUDNZD")

      }

   @Test
   fun `can do service lookups using TypedInstance from()`() {
      val (vyne, stub) = testVyne(
         """
            type Symbol inherits String
            type Quantity inherits Decimal
            type Price inherits Decimal
            type OrderCost inherits Decimal by Quantity * Price
            model Order {
               symbol : Symbol
               quantity : Quantity
            }
            model PricedOrder {
               symbol : Symbol
               quantity : Quantity
               cost : OrderCost
            }
            model SymbolPrice {
               symbol : Symbol
               price : Price
            }
            service PricingService {
               operation getPrice(Symbol):SymbolPrice
            }
         """.trimIndent()
      )
      val symbolPrice = vyne.parseJson("SymbolPrice", """{ "symbol" : "GBPNZD" , "price" : 0.48 }""")
      stub.addResponse("getPrice", listOf(symbolPrice), modifyDataSource = true)

      val order = TypedInstance.from(
         vyne.type("Order"),
         """{ "symbol" : "GBPNZD" , "quantity" :  100 }""",
         vyne.schema
      )
   }

   @Test
   fun `can resolve formula inputs that arent explicitly present on models`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         type OrderCost inherits Decimal by Quantity * Price
         type Price inherits Decimal
         type Symbol inherits String
         type Quantity inherits Decimal
         type Margin inherits Decimal by OrderCost * 1.1

         // Models
         model Order {
            symbol : Symbol
            quantity : Quantity
         }
         // This is the test - we don't have OrderCost on the
         // output model, but it's a required input into Margin.
         // OrderCost is discoverable from the other known facts though.
         model PricedOrder {
            symbol : Symbol
            margin : Margin
         }

         model SymbolPrice {
            price : Price
         }
         service PricingService {
            operation getPrice(Symbol):SymbolPrice
         }
      """.trimIndent()
      )
      stub.addResponse("getPrice", vyne.parseJson("SymbolPrice", """{ "price" : 0.8844 }"""), modifyDataSource = true)
      val order = vyne.parseJson(
         "Order", """{
            | "symbol" : "GBPNZD",
            | "quantity" : 1000000
            | }
         """.trimMargin()
      )
      val pricedOrder = vyne.from(order).build("PricedOrder")
         .typedObjects().single()
      val expectedMargin = (("1000000".toBigDecimal().multiply("0.8844".toBigDecimal())).multiply("1.1".toBigDecimal())) // (Quantity * Price) * 1.1 (the margin)
      pricedOrder.toRawObject().should.equal(
         mapOf(
            "symbol" to "GBPNZD",
            "margin" to expectedMargin
         )
      )
   }

   @Test
   fun `can resolve nested formula with multi hop lookups`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """ type Symbol inherits String
            type Quantity inherits Decimal
            type Price inherits Decimal
            type AgentCommissionRate inherits Decimal
            type ClientMarkupRate inherits Decimal

            // Formulas
            type OrderCost inherits Decimal by Quantity * Price
            type Margin inherits Decimal by OrderCost * ClientMarkupRate
            type Commission inherits Decimal by Margin * AgentCommissionRate
            type NetProfit inherits Decimal by Margin - Commission

            // Models
            model Order {
               clientId : ClientId inherits String
               symbol : Symbol
               quantity : Quantity
               salesUser : SalesUserId inherits Int
            }
            model PricedOrder {
               symbol : Symbol
               quantity : Quantity
               cost : OrderCost
               commission : Commission
               margin : Margin
               profit : NetProfit
            }
            model SymbolPrice {
               price : Price
            }
            [[ Models the markup that is applied for each customer ]]
            model MarkupSchedule {
               markup : ClientMarkupRate inherits Decimal
            }
            model CommissionSchedule {
               commissionRate : AgentCommissionRate
            }
            service PricingService {
               operation getPrice(Symbol):SymbolPrice
               operation getMarkupRate(ClientId):MarkupSchedule
               operation getCommissionSchedule(SalesUserId):CommissionSchedule
            }
         """.trimIndent()

      )
      // Set up stub calls
      stub.addResponse("getPrice", vyne.parseJson("SymbolPrice", """{ "price" : 0.8844 }"""), modifyDataSource = true)
      stub.addResponse(
         "getMarkupRate",
         vyne.parseJson("MarkupSchedule", """{ "markup" : 1.05 }"""),
         modifyDataSource = true
      )
      stub.addResponse(
         "getCommissionSchedule",
         vyne.parseJson("CommissionSchedule", """{ "commissionRate" : 0.02 }"""),
         modifyDataSource = true
      )

      val order = vyne.parseJson(
         "Order", """{
            | "clientId" : "client-1",
            | "symbol" : "GBPNZD",
            | "quantity" : 1000000,
            | "salesUser" : 1005
            | }
         """.trimMargin()
      )
      val built = vyne.from(order).build("PricedOrder")
         .typedObjects()
      val pricedOrder = built.single()
      val expected = mapOf(
         "symbol" to "GBPNZD",
         "quantity" to 1000000.toBigDecimal(),
         "cost" to "884400.0000".toBigDecimal(), // 1_000_000 * 0.8844
         "margin" to "928620.000000".toBigDecimal(), // 884400 *
         "commission" to "18572.40000000".toBigDecimal(), // 884400 * 0.02
         "profit" to "910047.60000000".toBigDecimal()
      )
      pricedOrder.toRawObject().should.equal(expected)

   }


   @Test
   fun `expressions are present on types`() {
      val schema = TaxiSchema.from(
         """
         type Height inherits Int
         type Width inherits Int
         type Area inherits Int by Height * Width
         """
      )
      val type = schema.type("Area")
      type.expression.should.not.be.`null`
      type.hasExpression.should.be.`true`
   }

   @Test
   fun `can evaluate simple expression type`() {
      val (vyne, _) = testVyne(
         """
         type Height inherits Int
         type Width inherits Int
         type Area inherits Int by Height * Width
         model Rectangle {
            height : Height
            width : Width
            area : Area
         }
      """.trimIndent()
      )
      val instance = vyne.parseJson("Rectangle", """{ "height" : 5 , "width" : 10 }""") as TypedObject
      instance.toRawObject().should.equal(
         mapOf(
            "height" to 5,
            "width" to 10,
            "area" to 50
         )
      )
   }


   @Test
   fun `can evaluate expression which adds literals`() {
      val (vyne, _) = testVyne(
         """
         type Height inherits Int
         type Width inherits Int
         type Area inherits Int by (Height * Width) + 5
         model Rectangle {
            height : Height
            width : Width
            area : Area
         }
      """.trimIndent()
      )
      val instance = vyne.parseJson("Rectangle", """{ "height" : 5 , "width" : 10 }""") as TypedObject
      instance.toRawObject().should.equal(
         mapOf(
            "height" to 5,
            "width" to 10,
            "area" to 55
         )
      )
   }

   @Test
   fun `can evaluate expression with function`() {
      val (vyne, _) = testVyne(
         """
            declare function squared(Int):Int

            type Height inherits Int

            type FunkyArea inherits Int by Height * squared(Height)

            model Rectangle {
               height : Height
               area : FunkyArea
            }
         """.trimIndent()
      )
      val functionRegistry = FunctionRegistry.default.add(
         functionOf("squared") { inputValues, _, returnType, _ ->
            val input = inputValues.first().value as Int
            val squared = input * input
            TypedValue.from(returnType, squared, source = Provided)
         }
      )
      val instance = vyne.parseJson(
         "Rectangle",
         """{ "height" : 5 , "width" : 10 }""",
         functionRegistry = functionRegistry
      ) as TypedObject
      instance.toRawObject().should.equal(
         mapOf(
            "height" to 5,
            "area" to 125
         )
      )
   }

   @Test
   fun `expression without all inputs evaluates to null`() {
      val (vyne, _) = testVyne(
         """
         type Height inherits Int
         type Width inherits Int
         type Area inherits Int by Height * Width
         model Rectangle {
            height : Height
//            width : Width // Exclude width
            area : Area
         }
      """.trimIndent()
      )
      val instance = vyne.parseJson("Rectangle", """{ "height" : 5 , "width" : 10 }""") as TypedObject
      val area = instance["area"]
      area.should.be.instanceof(TypedNull::class.java)
      area.source.should.be.instanceof(FailedEvaluatedExpression::class.java)
      val failedExpression = area.source as FailedEvaluatedExpression
      val expectedErrorMessage = """NumberCalculator doesn't support nulls, but some inputs were null:
Type Width was null - No attribute with type Width is present on type Rectangle"""
      failedExpression.errorMessage.withoutWhitespace().should.equal(expectedErrorMessage.withoutWhitespace())
   }
}
