package io.vyne.models.expressions

import com.winterbe.expekt.should
import io.vyne.models.FailedEvaluatedExpression
import io.vyne.models.Provided
import io.vyne.models.TypedNull
import io.vyne.models.TypedObject
import io.vyne.models.TypedValue
import io.vyne.models.functions.FunctionRegistry
import io.vyne.models.functions.functionOf
import io.vyne.models.json.parseJson
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.testVyne
import org.junit.Test

class ExpressionTest {

   @Test
   fun `can discover inputs from services`() {
      val (vyne,stub) = testVyne(
         """
            type Symbol inherits String
            type Quantity inherits Decimal
            type Price inherits Decimal
            type OrderCost inherits Decimal by Quantity * Price
            model Rfq {
               symbol : Symbol
               quantity : Quantity
               cost : OrderCost  // Order Cost requires Price, which is not present on this type, and requires discovery.
            }
            model Quote {
               symbol : Symbol
               price : Price
            }
         """.trimIndent()
      )
      val rfq = vyne.parseJson("Rfq", """{ "symbol" : "GBPNZD" , "quantity" :  100 }""")

      val quote = vyne.parseJson("Quote", """{ "symbol" : "GBPNZD" , "price" : 0.48 }""")
      vyne.addModel(quote)
      TODO()
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
      val (vyne,_) = testVyne(
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
            TypedValue.from(returnType,squared, source = Provided)
         }
      )
      val instance = vyne.parseJson("Rectangle", """{ "height" : 5 , "width" : 10 }""", functionRegistry = functionRegistry) as TypedObject
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
      failedExpression.errorMessage.should.equal("TODO")
   }
}
