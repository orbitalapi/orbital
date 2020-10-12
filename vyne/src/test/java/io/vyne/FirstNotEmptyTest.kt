package io.vyne

import com.winterbe.expekt.should
import io.vyne.models.*
import io.vyne.query.build.FirstNotEmptyPredicate
import io.vyne.schemas.Operation
import io.vyne.schemas.Parameter
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.types.PrimitiveType
import org.junit.Test
import java.time.LocalDate

class FirstNotEmptyTest {

   private val emptySchema = TaxiSchema.from("")
   @Test
   fun buildSpecMatchesWhenAnnotationIsPresent() {
      val type = TaxiSchema.from("""
         model TradeOutput {
            isin : String
            @FirstNotEmpty
            productName : String
         }
      """.trimIndent()).type("TradeOutput")
      FirstNotEmptyPredicate.provide(type.attribute("isin")).should.be.`null`
      FirstNotEmptyPredicate.provide(type.attribute("productName")).should.not.be.`null`
   }

   @Test
   fun buildSpecRejectsEmptyString() {
      FirstNotEmptyPredicate.isValid(
         instance(PrimitiveType.STRING, "")
      ).should.equal(false)
   }
   @Test
   fun buildSpecRejectsWhitepaceString() {
      FirstNotEmptyPredicate.isValid(
         instance(PrimitiveType.STRING, "   ")
      ).should.equal(false)
   }

   @Test
   fun buildSpecRejectsTypedNull() {
      FirstNotEmptyPredicate.isValid(
         instance(PrimitiveType.STRING, null)
      ).should.equal(false)
   }

   @Test
   fun buildSpecAcceptsString() {
      FirstNotEmptyPredicate.isValid(
         instance(PrimitiveType.STRING, "foo")
      ).should.equal(true)
   }

   @Test
   fun buildSpecAcceptsNumber() {
      FirstNotEmptyPredicate.isValid(
         instance(PrimitiveType.INTEGER, 123)
      ).should.equal(true)
   }

   @Test
   fun `when projecting and value is provided as null we do not attempt further discovery`() {
      val schema = TaxiSchema.from("""
         model TradeInput {
            isin : Isin as String
            productName : ProductName as String
         }
         service CalendarService {
            @StubResponse("lookupProduct")
            operation lookupDate(Isin):Product
         }
         model Product {
            name : ProductName
         }
         model TradeOutput {
            isin : Isin
            productName : ProductName
         }
      """.trimIndent())
      val (vyne, stubs) = testVyne(schema)
      val product = TypedInstance.from(schema.type("Product"), """{ "name": "ice cream" } """, schema, source = Provided)
      stubs.addResponse("lookupProduct", product)
      val inputJson = """{
         |"isin" : "1234",
         |"settlementDate" : null
         |}
      """.trimMargin()
      vyne.addModel(TypedInstance.from(schema.type("TradeInput"), inputJson, schema, source = Provided))
      val result = vyne.query().build("TradeOutput")
      val output = result["TradeOutput"] as TypedObject
      output["productName"].value.should.be.`null`
   }
   @Test
   fun `when projecting and value is tagged @FirstNotEmpty not provided on input, but is discoverable from a service, we discover it`() {
      val schema = TaxiSchema.from("""
         model TradeInput {
            isin : Isin as String
            productName : ProductName as String
         }
         service CalendarService {
            @StubResponse("lookupProduct")
            operation lookupDate(Isin):Product
         }
         model Product {
            name : ProductName
         }
         model TradeOutput {
            isin : Isin

            @FirstNotEmpty
            productName : ProductName
         }
      """.trimIndent())
      val (vyne, stubs) = testVyne(schema)
      val product = TypedInstance.from(schema.type("Product"), """{ "name": "ice cream" } """, schema, source = Provided)
      stubs.addResponse("lookupProduct", product)
      val inputJson = """{
         |"isin" : "1234",
         |"settlementDate" : null
         |}
      """.trimMargin()
      vyne.addModel(TypedInstance.from(schema.type("TradeInput"), inputJson, schema, source = Provided))
      val result = vyne.query().build("TradeOutput")
      val output = result["TradeOutput"] as TypedObject
      output["productName"].value.should.equal("ice cream")
   }

   @Test
   fun `FirstNotEmpty discovery works on formatted types`() {
      val schema = TaxiSchema.from("""
         type ExpiryDate inherits Date
         model TradeInput {
            isin : Isin as String
            expiryDate : ExpiryDate(@format = "dd-MMM-yy")
         }
         service CalendarService {
            @StubResponse("lookupDate")
            operation lookupDate(Isin):Product
         }
         model Product {
            expires : ExpiryDate
         }
         model TradeOutput {
            isin : Isin

            @FirstNotEmpty
            expiryDate : ExpiryDate(@format = "yyyy-MM-dd")
         }
      """.trimIndent())
      val (vyne, stubs) = testVyne(schema)
      val product = TypedInstance.from(schema.type("Product"), """{ "expires": "1979-05-10" } """, schema, source = Provided)
      stubs.addResponse("lookupDate", product)
      val inputJson = """{
         |"isin" : "1234"
         |}
      """.trimMargin()
      vyne.addModel(TypedInstance.from(schema.type("TradeInput"), inputJson, schema, source = Provided))
      val result = vyne.query().build("TradeOutput")
      val output = result["TradeOutput"] as TypedObject
      output["expiryDate"].value.should.equal(LocalDate.parse("1979-05-10"))
   }

   @Test
   fun `when value is tagged @FirstNotEmpty and multiple services expose it, if first service returns null, subsequent services are called`() {
      val schema = TaxiSchema.from("""
         model TradeInput {
            isin : Isin as String
            productName : ProductName as String
         }
         service CalendarService {
            @StubResponse("lookupProductA")
            operation lookupProductA(Isin):Product
            @StubResponse("lookupProductB")
            operation lookupProductB(Isin):Product

         }
         model Product {
            name : ProductName
         }
         model TradeOutput {
            isin : Isin

            @FirstNotEmpty
            productName : ProductName
         }
      """.trimIndent())
      val (vyne, stubs) = testVyne(schema)
      val product = TypedInstance.from(schema.type("Product"), """{ "name": "ice cream" } """, schema, source = Provided)
      var counter :Int = 0
      val firstResponderReturnsNullHandler:StubResponseHandler = { operation: Operation, list: List<Pair<Parameter, TypedInstance>> ->
         if (counter == 0) {
            counter++
            TypedNull(schema.type("Product"))
         } else {
            product
         }
      }
      stubs.addResponse("lookupProductA", firstResponderReturnsNullHandler)
      stubs.addResponse("lookupProductB", firstResponderReturnsNullHandler)
      val inputJson = """{
         |"isin" : "1234",
         |"settlementDate" : null
         |}
      """.trimMargin()
      vyne.addModel(TypedInstance.from(schema.type("TradeInput"), inputJson, schema, source = Provided))
      val result = vyne.query().build("TradeOutput")
      val output = result["TradeOutput"] as TypedObject
      output["productName"].value.should.equal("ice cream")
   }



   @Test
   fun `when value is tagged @FirstNotEmpty and multiple services expose it, if first service returns a value but the attribute is null, subsequent services are called`() {
      val schema = TaxiSchema.from("""
         model TradeInput {
            isin : Isin as String
            productName : ProductName as String
         }
         service CalendarService {
            @StubResponse("lookupProductA")
            operation lookupProductA(Isin):Product
            @StubResponse("lookupProductB")
            operation lookupProductB(Isin):Product

         }
         model Product {
            name : ProductName
         }
         model TradeOutput {
            isin : Isin

            @FirstNotEmpty
            productName : ProductName
         }
      """.trimIndent())
      val (vyne, stubs) = testVyne(schema)
      var counter :Int = 0
      val firstResponderReturnsNullHandler:StubResponseHandler = { operation: Operation, list: List<Pair<Parameter, TypedInstance>> ->
         if (counter == 0) {
            counter++
            // First time, return null in the name attribute
            TypedInstance.from(schema.type("Product"), """{ "name": null } """, schema, source = Provided)
         } else {
            TypedInstance.from(schema.type("Product"), """{ "name": "ice cream" } """, schema, source = Provided)
         }
      }
      stubs.addResponse("lookupProductA", firstResponderReturnsNullHandler)
      stubs.addResponse("lookupProductB", firstResponderReturnsNullHandler)
      val inputJson = """{
         |"isin" : "1234",
         |"settlementDate" : null
         |}
      """.trimMargin()
      vyne.addModel(TypedInstance.from(schema.type("TradeInput"), inputJson, schema, source = Provided))
      val result = vyne.query().build("TradeOutput")
      val output = result["TradeOutput"] as TypedObject
      output["productName"].value.should.equal("ice cream")
   }


   @Test
   fun `when projecting a collection and operation fails for first entry but succeeds for second then value is still populated`() {
      val schema = TaxiSchema.from("""
         model TradeInput {
            isin : Isin as String
            productName : ProductName as String
         }
         service CalendarService {
            @StubResponse("lookupProduct")
            operation lookupProduct(Isin):Product
         }
         model Product {
            name : ProductName
         }
         model TradeOutput {
            isin : Isin

            @FirstNotEmpty
            productName : ProductName
         }
      """.trimIndent())
      val (vyne, stubs) = testVyne(schema)
      val firstResponderReturnsNullHandler:StubResponseHandler = { operation: Operation, inputs: List<Pair<Parameter, TypedInstance>> ->
         val inputParam = inputs[0].second.value as String
         if (inputParam == "productA") {
            // First time, return null in the name attribute
            TypedInstance.from(schema.type("Product"), """{ "name": null } """, schema, source = Provided)
         } else {
            TypedInstance.from(schema.type("Product"), """{ "name": "ice cream" } """, schema, source = Provided)
         }
      }
      stubs.addResponse("lookupProduct", firstResponderReturnsNullHandler)
      val inputJson = """[{
         |"isin" : "productA",
         |"productName" : null
         |},
         |{
         |"isin" : "productB",
         |"productName" : null
         |}]
      """.trimMargin()
      val inputModel = TypedInstance.from(schema.type("TradeInput[]"), inputJson, schema, source = Provided)
      vyne.addModel(inputModel)
      val result = vyne.query().build("TradeOutput[]")
      val output = result["TradeOutput[]"] as TypedCollection
      val transformedProductA = output.first { (it as TypedObject)["isin"].value == "productA" } as TypedObject
      val transformedProductB = output.first { (it as TypedObject)["isin"].value == "productB" } as TypedObject
      // Note to future self:  I suspect we'll change this at some point so the attribute
      // is there, but null
      transformedProductA["productName"].value.should.be.`null`
      transformedProductB["productName"].value.should.equal("ice cream")
   }


   private fun instance(type:PrimitiveType, value:Any?):TypedInstance {
      return if (value == null) {
         TypedNull(emptySchema.type(type.qualifiedName))
      } else {
         TypedInstance.from(emptySchema.type(type.qualifiedName), value, emptySchema, source = Provided)
      }
   }
}
