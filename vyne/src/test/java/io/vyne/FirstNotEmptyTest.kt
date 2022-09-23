package io.vyne

import app.cash.turbine.test
import app.cash.turbine.testIn
import com.winterbe.expekt.should
import io.vyne.models.Provided
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.models.TypedObject
import io.vyne.models.json.parseJsonModel
import io.vyne.query.build.FirstNotEmptyPredicate
import io.vyne.query.connectors.OperationResponseHandler
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.taxi.TaxiSchema
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import lang.taxi.types.PrimitiveType
import org.junit.Test
import java.time.LocalDate
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalCoroutinesApi
class FirstNotEmptyTest {

   private val emptySchema = TaxiSchema.from("")

   @Test
   fun buildSpecMatchesWhenAnnotationIsPresent() = runBlockingTest {
      val type = TaxiSchema.from(
         """
         model TradeOutput {
            isin : String
            @FirstNotEmpty
            productName : String
         }
      """.trimIndent()
      ).type("TradeOutput")
      FirstNotEmptyPredicate.provide(type.attribute("isin")).should.be.`null`
      FirstNotEmptyPredicate.provide(type.attribute("productName")).should.not.be.`null`
   }

   @Test
   fun buildSpecRejectsEmptyString() = runBlockingTest {
      FirstNotEmptyPredicate.isValid(
         instance(PrimitiveType.STRING, "")
      ).should.equal(false)
   }

   @Test
   fun buildSpecRejectsWhitepaceString() = runBlockingTest {
      FirstNotEmptyPredicate.isValid(
         instance(PrimitiveType.STRING, "   ")
      ).should.equal(false)
   }

   @Test
   fun buildSpecRejectsTypedNull() = runBlockingTest {
      FirstNotEmptyPredicate.isValid(
         instance(PrimitiveType.STRING, null)
      ).should.equal(false)
   }

   @Test
   fun buildSpecAcceptsString() = runBlockingTest {
      FirstNotEmptyPredicate.isValid(
         instance(PrimitiveType.STRING, "foo")
      ).should.equal(true)
   }

   @Test
   fun buildSpecAcceptsNumber() = runBlockingTest {
      FirstNotEmptyPredicate.isValid(
         instance(PrimitiveType.INTEGER, 123)
      ).should.equal(true)
   }

   @Test
   fun `when projecting and value is provided as null we do not attempt further discovery`() = runBlockingTest {
      val schema = TaxiSchema.from(
         """
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
      """.trimIndent()
      )
      val (vyne, stubs) = testVyne(schema)
      val product =
         TypedInstance.from(schema.type("Product"), """{ "name": "ice cream" } """, schema, source = Provided)
      stubs.addResponse("lookupProduct", product)
      val inputJson = """{
         |"isin" : "1234",
         |"settlementDate" : null
         |}
      """.trimMargin()
      vyne.addModel(TypedInstance.from(schema.type("TradeInput"), inputJson, schema, source = Provided))
      val result = vyne.query().build("TradeOutput")
      val output = result.firstTypedObject()
      output["productName"].value.should.be.`null`
   }

   @Test
   fun `when projecting and value is tagged @FirstNotEmpty not provided on input, but is discoverable from a service, we discover it`() =
      runBlocking {
         val schema = TaxiSchema.from(
            """
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
      """.trimIndent()
         )
         val (vyne, stubs) = testVyne(schema)
         val product =
            TypedInstance.from(schema.type("Product"), """{ "name": "ice cream" } """, schema, source = Provided)
         stubs.addResponse("lookupProduct", product)
         val inputJson = """{
         |"isin" : "1234",
         |"settlementDate" : null
         |}
      """.trimMargin()
         vyne.addModel(TypedInstance.from(schema.type("TradeInput"), inputJson, schema, source = Provided))
         val result = vyne.query().build("TradeOutput")
         result.results.test {
            val output = expectTypedObject()
            output["productName"].value.should.equal("ice cream")
            awaitComplete()
         }

      }

   @Test
   fun `FirstNotEmpty discovery works on formatted types`() = runBlocking {
      val schema = TaxiSchema.from(
         """
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
      """.trimIndent()
      )
      val (vyne, stubs) = testVyne(schema)
      val product =
         TypedInstance.from(schema.type("Product"), """{ "expires": "1979-05-10" } """, schema, source = Provided)
      stubs.addResponse("lookupDate", product)
      val inputJson = """{
         |"isin" : "1234"
         |}
      """.trimMargin()
      vyne.addModel(TypedInstance.from(schema.type("TradeInput"), inputJson, schema, source = Provided))
      val result = vyne.query().build("TradeOutput")
      result.results.test {
         val output = expectTypedObject()
         output["expiryDate"].value.should.equal(LocalDate.parse("1979-05-10"))
         awaitComplete()
      }

   }

   @Test
   fun `when value is tagged @FirstNotEmpty and multiple services expose it, if first service returns null, subsequent services are called`() =
      runBlocking {
         val schema = TaxiSchema.from(
            """
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
      """.trimIndent()
         )
         val (vyne, stubs) = testVyne(schema)
         val product =
            TypedInstance.from(schema.type("Product"), """{ "name": "ice cream" } """, schema, source = Provided)
         var counter: Int = 0
         val firstResponderReturnsNullHandler: OperationResponseHandler =
            { _: RemoteOperation, _: List<Pair<Parameter, TypedInstance>> ->
               if (counter == 0) {
                  counter++
                  listOf(TypedNull.create(schema.type("Product")))
               } else {
                  listOf(product)
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

         result
            .results.test {
               expectTypedObject()["productName"].value.should.equal("ice cream")
               awaitComplete()
            }

      }

   @Test
   fun `when type is present twice on a model through inheritence and one value is populated, then it is returned`() =
      runBlockingTest {
         val (vyne, stub) = testVyne(
            """
         type Name inherits String
         type Id inherits Int
         model NamedThing {
            name : Name
         }
         model Person inherits NamedThing {
            firstName : Name
         }
         model OutputModel {
            @FirstNotEmpty
            discoveredName : Name
         }
      """.trimIndent()
         )
         val personWithBaseTypeName = vyne.parseJsonModel("Person", """{ "firstName" : null, "name" : "Jimmy" }""")


         vyne.from(personWithBaseTypeName).build("OutputModel").let { queryResult ->
            val outputModel = queryResult.firstTypedObject()
            outputModel["discoveredName"].value.should.equal("Jimmy")
         }

         val personWithFirstName = vyne.parseJsonModel("Person", """{ "firstName" : "Jimmy" , "name" : null }""")

         vyne.from(personWithFirstName).build("OutputModel").let { queryResult ->
            val outputModel = queryResult.firstTypedObject()
            outputModel["discoveredName"].value.should.equal("Jimmy")
         }
      }

   @Test
   fun `when type is present twice on a model through inheritence but only one value is populated, and the model is returned from a service, then the values from the service are present on query results`() =
      runBlocking {
         val (vyne, stub) = testVyne(
            """
         type Name inherits String
         type Id inherits Int
         model NamedThing {
            name : Name
         }
         model Person inherits NamedThing {
            firstName : Name
         }
         model OutputModel {
            id : Id
            @FirstNotEmpty
            discoveredName : Name
         }
         service PersonIdService {
            operation findAllIds():Id[]
         }
         service PersonService {
            operation findPerson(Id):Person
         }
      """.trimIndent()
         )
//      stub.addResponse("findAllIds", TypedCollection.from(listOf(1, 2).map { vyne.typedValue("Id", it) }))
         stub.addResponse("findAllIds", TypedCollection.from(listOf(1).map { vyne.typedValue("Id", it) }))

         val personWithBaseTypeName =
            vyne.parseJsonModel("Person", """{ "firstName" : null, "name" : "Jimmy BaseName" }""")
         val personWithFirstName =
            vyne.parseJsonModel("Person", """{ "firstName" : "Jimmy FirstName" , "name" : null }""")
         stub.addResponse("findPerson") { remoteOperation, params ->
            val (_, personId) = params[0]
            when (personId.value) {
               1 -> listOf(personWithBaseTypeName)
               2 -> listOf(personWithFirstName)
               else -> error("Expected Id of 1 or 2")
            }
         }
         runTest {
            val turbine = vyne.query("find { Id[] } as OutputModel[]").rawResults.testIn(this)
            // There are two Name types present - Name (the base type), and FirstName (the subtype).
            // Person1 has their Name (basetype) populated in the service response
            turbine.expectRawMap().should.equal(mapOf("id" to 1, "discoveredName" to "Jimmy BaseName"))

            // TODO Why is this commented out?
            // Person2 has their FirstName (subtype) populated in the service response.
            // expectRawMap().should.equal(mapOf("id" to 2, "discoveredName" to "Jimmy FirstName"))
            turbine.awaitComplete()
         }
      }


   @Test
   fun `when value is tagged @FirstNotEmpty and multiple services expose it, if first service returns a value but the attribute is null, subsequent services are called`() =
      runBlocking {
         val schema = TaxiSchema.from(
            """
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
      """.trimIndent()
         )
         val (vyne, stubs) = testVyne(schema)
         var counter: Int = 0
         val firstResponderReturnsNullHandler: OperationResponseHandler =
            { operation: RemoteOperation, list: List<Pair<Parameter, TypedInstance>> ->
               if (counter == 0) {
                  counter++
                  // First time, return null in the name attribute
                  listOf(TypedInstance.from(schema.type("Product"), """{ "name": null } """, schema, source = Provided))
               } else {
                  listOf(
                     TypedInstance.from(
                        schema.type("Product"),
                        """{ "name": "ice cream" } """,
                        schema,
                        source = Provided
                     )
                  )
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
         result.results.test {
            val output = expectTypedObject()
            output["productName"].value.should.equal("ice cream")
            awaitComplete()
         }
      }


   @Test
   fun `when projecting a collection and operation fails for first entry but succeeds for second then value is still populated`() =
      runBlocking {
         val schema = TaxiSchema.from(
            """
         model TradeInput {
            isin : Isin as String
            productName : ProductName as String
         }
         service ProductService {
            @StubResponse("lookupProduct")
            operation lookupProduct(Isin):Product
         }
         model Product {
            @Id
            isin: Isin
            name : ProductName
         }
         model TradeOutput {
            isin : Isin

            @FirstNotEmpty
            productName : ProductName
         }
      """.trimIndent()
         )
         val (vyne, stubs) = testVyne(schema)
         val firstResponderReturnsNullHandler: OperationResponseHandler =
            { operation: RemoteOperation, inputs: List<Pair<Parameter, TypedInstance>> ->
               val inputParam = inputs[0].second.value as String
               if (inputParam == "productA") {
                  // First time, return null in the name attribute
                  listOf(TypedInstance.from(schema.type("Product"), """{ "name": null } """, schema, source = Provided))
               } else {
                  listOf(
                     TypedInstance.from(
                        schema.type("Product"),
                        """{ "name": "ice cream" } """,
                        schema,
                        source = Provided
                     )
                  )
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
         result.results.test {
            val transformedProductA = expectTypedObject()
            // Note to future self:  I suspect we'll change this at some point so the attribute
            // is there, but null
            transformedProductA["productName"].value.should.be.`null`

            val transformedProductB = expectTypedObject()
            transformedProductB["productName"].value.should.equal("ice cream")

            awaitComplete()
         }

      }

   @Test
   fun `when an anonymous field  is tagged @FirstNotEmpty and multiple services expose it, if first service returns null, subsequent services are called`() =
      runBlocking {
         val schema = TaxiSchema.from(
            """
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


            productName : {
              @FirstNotEmpty
              name: ProductName
            }
         }
      """.trimIndent()
         )
         val (vyne, stubs) = testVyne(schema)
         val product =
            TypedInstance.from(schema.type("Product"), """{ "name": "ice cream" } """, schema, source = Provided)
         var counter: Int = 0
         val firstResponderReturnsNullHandler: OperationResponseHandler =
            { _: RemoteOperation, _: List<Pair<Parameter, TypedInstance>> ->
               if (counter == 0) {
                  counter++
                  listOf(TypedNull.create(schema.type("Product")))
               } else {
                  listOf(product)
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
         result.results.test {
            val item = expectTypedObject()
            val productNameAnonymousType = item["productName"] as TypedObject
            productNameAnonymousType["name"].value.should.equal("ice cream")
            awaitComplete()
         }


      }


   private fun instance(type: PrimitiveType, value: Any?): TypedInstance {
      return if (value == null) {
         TypedNull.create(emptySchema.type(type.qualifiedName))
      } else {
         TypedInstance.from(emptySchema.type(type.qualifiedName), value, emptySchema, source = Provided)
      }
   }

}


