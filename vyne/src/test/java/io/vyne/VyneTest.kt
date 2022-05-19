package io.vyne

import app.cash.turbine.test
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.models.DataSource
import io.vyne.models.FailedEvaluatedExpression
import io.vyne.models.Provided
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.models.TypedValue
import io.vyne.models.functions.FunctionRegistry
import io.vyne.models.json.addJson
import io.vyne.models.json.addJsonModel
import io.vyne.models.json.addKeyValuePair
import io.vyne.models.json.parseJson
import io.vyne.models.json.parseJsonCollection
import io.vyne.models.json.parseJsonModel
import io.vyne.models.json.parseKeyValuePair
import io.vyne.query.QueryContext
import io.vyne.query.QueryEngineFactory
import io.vyne.query.QueryParser
import io.vyne.query.QueryResult
import io.vyne.query.QuerySpecTypeNode
import io.vyne.query.TypeNameQueryExpression
import io.vyne.query.connectors.OperationInvoker
import io.vyne.query.graph.operationInvocation.CacheAwareOperationInvocationDecorator
import io.vyne.schemas.Operation
import io.vyne.schemas.Type
import io.vyne.schemas.taxi.TaxiSchema
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Ignore
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.time.Instant
import java.time.LocalDate
import java.util.*
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.ExperimentalTime


object TestSchema {
   val taxiDef = """
namespace vyne.example
type Invoice {
   clientId : ClientId
   invoiceValue : InvoiceValue as Decimal
}
type Client {
   clientId : ClientId as String
   name : ClientName as String
   isicCode : IsicCode as String
}
type alias TaxFileNumber as String
type alias CreditRisk as Int
type alias NaicsCode as Int

service ClientService {
   @StubResponse("mockClient")
   operation getClient(TaxFileNumber):Client

   @StubResponse("creditRisk")
   operation getCreditRisk(ClientId,InvoiceValue):CreditRisk

   @StubResponse("mockClients")
   operation getClients(NaicsCode):Client[]
}
"""
   val schema = TaxiSchema.from(taxiDef)


   fun vyne(
      queryEngineFactory: QueryEngineFactory = QueryEngineFactory.default(),
      testSchema: TaxiSchema = schema
   ) = Vyne(listOf(testSchema), queryEngineFactory)

   val queryParser = QueryParser(schema)

   fun typeNode(name: String, parser: QueryParser = queryParser): Set<QuerySpecTypeNode> {
      return parser.parse(TypeNameQueryExpression(name))
   }

   fun queryContext(queryId: String = UUID.randomUUID().toString()): QueryContext =
      vyne().queryEngine().queryContext(queryId = queryId, clientQueryId = null)
}

fun testVyne(schema: TaxiSchema): Pair<Vyne, StubService> {
   val stubService = StubService(schema = schema)
   val queryEngineFactory =
      QueryEngineFactory.withOperationInvokers(VyneCacheConfiguration.default(), emptyList(), stubService)
   val vyne = Vyne(listOf(schema), queryEngineFactory)
   return vyne to stubService
}

fun testVyneWithStub(schema: TaxiSchema, invokers: List<OperationInvoker> = emptyList()): Pair<Vyne, StubService> {
   val stubService = StubService(schema = schema)
   val queryEngineFactory = QueryEngineFactory.withOperationInvokers(
      VyneCacheConfiguration.default(),
      emptyList(),
      stubService,
      *invokers.toTypedArray()
   )
   val vyne = Vyne(listOf(schema), queryEngineFactory)
   return vyne to stubService
}


fun testVyne(schema: String, invokerProvider: (TaxiSchema) -> List<OperationInvoker>): Vyne {
   return testVyne(listOf(schema), invokerProvider)
}

fun testVyneWithStub(schema: String, invokerProvider: (TaxiSchema) -> List<OperationInvoker>): Pair<Vyne, StubService> {
   return testVyneWithStub(listOf(schema), invokerProvider)
}

fun testVyne(schemas: List<String>, invokerProvider: (TaxiSchema) -> List<OperationInvoker>): Vyne {
   val schema = TaxiSchema.fromStrings(schemas)
   val invokers = invokerProvider(schema)
   return testVyne(schema, invokers)
}

fun testVyneWithStub(
   schemas: List<String>,
   invokerProvider: (TaxiSchema) -> List<OperationInvoker>
): Pair<Vyne, StubService> {
   val schema = TaxiSchema.fromStrings(schemas)
   val invokers = invokerProvider(schema)
   return testVyneWithStub(schema, invokers)
}

fun testVyne(schemas: List<String>, invokers: List<OperationInvoker>): Vyne {
   return testVyne(TaxiSchema.fromStrings(schemas), invokers)
}

fun testVyne(schema: String, invokers: List<OperationInvoker>): Vyne {
   return testVyne(TaxiSchema.from(schema), invokers)
}

fun testVyne(schema: TaxiSchema, invokers: List<OperationInvoker>): Vyne {
   val queryEngineFactory = QueryEngineFactory.withOperationInvokers(VyneCacheConfiguration.default(), invokers)
   return Vyne(queryEngineFactory).addSchema(schema)
}

fun testVyne(vararg schemas: String): Pair<Vyne, StubService> {
   return testVyne(TaxiSchema.fromStrings(schemas.toList()))
}

fun testVyneWithStub(schema: String, invokers: List<OperationInvoker>): Pair<Vyne, StubService> {
   return testVyneWithStub(TaxiSchema.from(schema), invokers)
}

fun testVyne(schema: String, functionRegistry: FunctionRegistry = FunctionRegistry.default) = testVyne(TaxiSchema.compileOrFail(schema, functionRegistry = functionRegistry))

@ExperimentalTime
@ExperimentalCoroutinesApi
class VyneTest {

   @Test
   fun `when one operation failed but another path is present with different inputs then the different path is tried`() =
      runBlocking {
         val (vyne, stubs) = testVyne(
            """
         type AssetClass inherits String
         type Puid inherits Int
         type InstrumentId inherits String
         type CfiCode inherits String
         type Isin inherits String
         model Output {
            @FirstNotEmpty assetClass: AssetClass
            @FirstNotEmpty puid: Puid
         }

         model Input {
            instrumentId: InstrumentId
         }

         model Instrument {
            instrumentId: InstrumentId
            cifCode: CfiCode
            isin: Isin
         }

         model CfiToPuid {
            cifCode: CfiCode
            puid: Puid
         }

         model Product {
            puid: Puid
            assetClass: AssetClass
         }

         model AnnaResponse {
            isin : Isin
            derClassificationType : CfiCode
         }

         service InstrumentService {
            @StubOperation("findByInstrumentId")
            operation findByInstrumentId(InstrumentId):Instrument
         }

         // This is the service that will conditionally fail.
         // There are two paths to finding inputs.
         // The first (shorter) path will fail, and we want
         // to ensure that the second longer path is also evaluated.
         service CfiToPuidCaskService {
            @StubOperation("findByCfiCode")
            operation findByCfiCode(CfiCode):CfiToPuid
         }

         service ProductService {
            @StubOperation("findByPuid")
            operation findByPuid(Puid):Product
         }

         service AnnaService {
            @StubOperation("findByIsin")
            operation findByIsin(Isin):AnnaResponse
         }

         service InputService {
           @StubOperation("findAll")
            operation `findAll`(): Input[]
         }
      """.trimIndent()
         )

         // This test contains an operation (CfiToPuidCaskService.findByCfiCode)
         // which has two different paths for evaluation.
         // The first (shorter path) gets it's input from
         // InstrumentService -> cfiCode -> CfiToPuidCaskService@@findByCfiCode
         // We've set that path to fail.
         // The second path is:
         // InstrumentService -> isin -> AnnaService -> cfiCode -> CfiToPuidCaskService
         // That path, if evaluated, will succeed

         val inputJson = """[{"instrumentId" : "InstrumentId"}]""".trimMargin()
         val inputs = TypedInstance.from(vyne.type("Input[]"), inputJson, vyne.schema, source = Provided)
         val instrument = """{
         |"instrumentId": "InstrumentId",
         |"cifCode": "XXXX",
         |"isin": "Isin"
         |}
      """.trimMargin()

         stubs.addResponse("`findAll`", inputs)

         stubs.addResponse(
            "findByInstrumentId",
            TypedInstance.from(vyne.type("Instrument"), instrument, vyne.schema, source = Provided)
         )

         stubs.addResponse("findByCfiCode") { operation, parameters ->
            val cfiCode = parameters[0].second
            if (cfiCode.value != "XXXX") {
               val response = """{
               |"puid" : 519,
               |"cfiCode" : "$cfiCode"
               |}
            """.trimMargin()
               listOf(TypedInstance.from(vyne.type("Product"), response, vyne.schema, source = Provided))
            } else {
               throw IllegalArgumentException()
            }
         }

         stubs.addResponse(
            "findByPuid",
            TypedInstance.from(
               vyne.type("Product"), """{
            |"puid": 519,
            |"assetClass": "assetClass"
            |}""".trimMargin(), vyne.schema, source = Provided
            )
         )

         val annaResponse = vyne.parseJsonModel(
            "AnnaResponse", """{
            |"isin": "Isin",
            |"derClassificationType": "SCABC"
            |}""".trimMargin()
         )
         stubs.addResponse(
            "findByIsin",
            annaResponse
         )

         val queryResult = vyne.query(
            """
         findAll { Input[] }  as Output[]
      """.trimIndent()
         )
         queryResult.results.test(Duration.INFINITE) {
            val typedInstance = expectTypedObject()
            typedInstance["puid"].value.should.not.be.`null`
            typedInstance["assetClass"].value.should.not.be.`null`
            awaitComplete()
         }
      }

   @Test
   fun `when a provided object has a typed null for a value, it shouldnt be used as an input`() {
      val (vyne, stubs) = testVyne(
         """
         model Order {
            cfiCode : CfiCode? as String
            isin : Isin as String
         }
         model Product {
            productId : ProductId as Int
            cfiCode : CfiCode
         }

         model CfiCodeHolder {
            cfiCode: CfiCode
            field: String
         }

         service ProductService {
            // Shortest path, but provided value is null, so shouldn't be called
            @StubOperation("findByCfiCode")
            operation findByCfiCode(CfiCode):Product
            // Longer path, but returns correct value
            @StubOperation("isinToCfi")
            operation isinToCfi(Isin):CfiCodeHolder
         }
      """.trimIndent()
      )
      val inputJson = """{
         |"cfiCode" : null,
         |"isin" : "isin-123"
         |}
      """.trimMargin()
      val input = TypedInstance.from(vyne.type("Order"), inputJson, vyne.schema, source = Provided)

      val cfiCodeHolder = """{
         |"cfiCode": "Cfi-123",
         |"field": "value"
         |}
      """.trimMargin()
      stubs.addResponse(
         "isinToCfi",
         TypedInstance.from(vyne.type("CfiCodeHolder"), cfiCodeHolder, vyne.schema, source = Provided)
      )
      stubs.addResponse("findByCfiCode") { operation, parameters ->
         val cfiCode = parameters[0].second
         if (cfiCode.value == "Cfi-123") {
            val response = """{
               |"productId" : 123,
               |"cfiCode" : "Cfi-123"
               |}
            """.trimMargin()
            listOf(TypedInstance.from(vyne.type("Product"), response, vyne.schema, source = Provided))
         } else {
            fail("findByCfiCode called using the wrong parameter -- should've resolve against Isin first")
         }
      }
      runBlocking {
         val queryResult = vyne.from(input).find("ProductId")
         queryResult.isFullyResolved.should.be.`true`
         queryResult.typedInstances().first().value.should.equal(123)
      }
   }

   @Test
   fun `calls remote services to discover response from deeply nested value`() {
      val (vyne, stubs) = testVyne(
         """
         namespace vyne.tests {
            type Isin inherits String
            type SecurityDescription inherits String
            model InstrumentResponse {
                isin : Isin?
                annaJson : AnnaJson?
            }
            model AnnaJson {
                Derived : Derived?
            }
            model Derived {
                ShortName : SecurityDescription?
            }

            model RequiredOutput {
               isin : Isin?
               description : SecurityDescription?
            }

            service StubService {
               @StubResponse("securityDescription")
               operation getAnnaJson(isin:Isin):InstrumentResponse
            }
         }
      """
      )
      val stubResponse = TypedInstance.from(
         vyne.type("vyne.tests.InstrumentResponse"), """
         {
            "isin": "foo",
            "annaJson" : {
               "Derived" : {
                  "ShortName" : "Jimmy's Diner"
               }
            }
         }
      """.trimIndent(), vyne.schema, source = Provided
      )

      runBlocking {
         stubs.addResponse("securityDescription", stubResponse)
         vyne.addKeyValuePair("vyne.tests.Isin", "foo")
         val result = vyne.query().build("vyne.tests.RequiredOutput")
         result.isFullyResolved.should.be.`true`
         val rawResult = result.rawObjects().first()
         val resultJson = jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(rawResult)
         val expected = """{
         | "isin" : "foo",
         | "description" : "Jimmy's Diner"
         | }
      """.trimMargin()
         JSONAssert.assertEquals(expected, resultJson, true)
      }
   }

   @Test
   fun shouldFindAPropertyOnAnObject() = runBlockingTest {

      val vyne = TestSchema.vyne()
      val json = """
{
   "clientId" : "123",
   "name" : "Jimmy's Choos",
   "isicCode" : "retailer"
}"""
      vyne.addJsonModel("vyne.example.Client", json)
      val queryResult = vyne.query().find("vyne.example.ClientName")
      queryResult.typedInstances().let { result ->
         result.should.have.size(1)
         result.first().value.should.equal("Jimmy's Choos")
      }
   }

   @Test
   fun `vyne should invoke services using value from enum synonym`() {
      val enumSchema = TaxiSchema.from(
         """
                namespace companyX {
                   model Product {
                     name : String
                  }
                  enum ProductType {
                     SPOT(919),
                     FORWARD(920)
                  }
                  service ProductTaxonomyService {
                     @StubResponse("mockProduct")
                     operation getProduct(ProductType):Product
                  }
                }
                namespace vendorA {
                   enum ProductType {
                      FX_SPOT("Spot") synonym of companyX.ProductType.SPOT
                   }
                }

      """.trimIndent()
      )
      val (vyne, stubService) = testVyne(enumSchema)
      val product = vyne.parseJsonModel(
         "companyX.Product", """
         {
            "name": "USD/GBP"
         }
      """.trimIndent()
      )

      stubService.addResponse("mockProduct") { _, parameters ->
         parameters.should.have.size(1)
         parameters.first().second.value.should.be.equal(919)
         listOf(product)
      }
      val instance = TypedInstance.from(vyne.schema.type("vendorA.ProductType"), "Spot", vyne.schema, source = Provided)
      vyne.addModel(instance)
      runBlocking {
         val queryResult = vyne.query().find("companyX.Product")
         val attributeMap = queryResult.rawObjects().first()
            .should.equal(
               mapOf(
                  "name" to "USD/GBP"
               )
            )
      }
   }

   @Test
   fun `vyne should emit values transitively that conform to the enum spec`() {
      val enumSchema = TaxiSchema.from(
         """
                namespace companyY {
                   model Product {
                     name : String
                  }

                  enum ProductClassification {
                     T_PLUS_2("FX_T2"),
                     T_PLUS_N("FX_TN")
                  }

                  service ProductTaxonomyService {
                     @StubResponse("mockProduct")
                     operation getProduct(ProductClassification):Product
                  }
                }
                namespace companyX {
                   model Product {
                     name : String
                  }
                  enum ProductType {
                     SPOT(919) synonym of companyY.ProductClassification.T_PLUS_2,
                     FORWARD(920)
                  }
                }
                namespace vendorA {
                   enum ProductType {
                      FX_SPOT("Spot") synonym of companyX.ProductType.SPOT
                   }
                }

      """.trimIndent()
      )

      val (vyne, stubService) = testVyne(enumSchema)
      val product = vyne.parseJson(
         "companyY.Product", """
         {
            "name": "USD/GBP"
         }
      """.trimIndent()
      )
      stubService.addResponse("mockProduct") { _, parameters ->
         parameters.should.have.size(1)
         parameters.first().second.value.should.be.equal("FX_T2")
         listOf(product)
      }
      val instance = TypedInstance.from(vyne.schema.type("vendorA.ProductType"), "Spot", vyne.schema, source = Provided)
      vyne.addModel(instance)

      runBlocking {
         val queryResult = vyne.query().find("companyY.Product")
         queryResult
            .rawObjects().first()
            .should.equal(
               mapOf(
                  "name" to "USD/GBP"
               )
            )
      }
   }

   @Test
   fun shouldRetrievePropertyFromService() {
      val stubService = StubService()
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(VyneCacheConfiguration.default(), emptyList(), stubService)
      val vyne = TestSchema.vyne(queryEngineFactory)

      val json = """
{
   "clientId" : "123",
   "name" : "Jimmy's Choos",
   "isicCode" : "retailer"
}"""

      runBlocking {
         val client = vyne.parseJsonModel("vyne.example.Client", json)
         stubService.addResponse("mockClient", client)
         vyne.addKeyValuePair("vyne.example.TaxFileNumber", "123")
         val result: QueryResult = vyne.query().find("vyne.example.ClientName")
         result.typedInstances().first().value.should.equal("Jimmy's Choos")
      }
   }

   @Test
   fun shouldBeAbleToQueryWithShortNames() {
      val stubService = StubService()
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(VyneCacheConfiguration.default(), emptyList(), stubService)
      val vyne = TestSchema.vyne(queryEngineFactory)

      val json = """
{
   "clientId" : "123",
   "name" : "Jimmy's Choos",
   "isicCode" : "retailer"
}"""
      runBlocking {
         val client = vyne.parseJsonModel("Client", json)
         stubService.addResponse("mockClient", client)
         vyne.addKeyValuePair("vyne.example.TaxFileNumber", "123")
         val result: QueryResult = vyne.query().find("ClientName")
         result.typedInstances().first().value.should.equal("Jimmy's Choos")
      }
   }

   @Test
   fun shouldRetrievePropertyFromService_withMultipleAttributes_whenAttributesArePresentAsKeyValuePairs() {
      val stubService = StubService()
      val queryEngineFactory =
         QueryEngineFactory.withOperationInvokers(VyneCacheConfiguration.default(), emptyList(), stubService)
      val vyne = TestSchema.vyne(queryEngineFactory)
      stubService.addResponse(
         "creditRisk",
         TypedValue.from(vyne.getType("vyne.example.CreditRisk"), 100, source = Provided)
      )
      vyne.addKeyValuePair("vyne.example.ClientId", "123")
      vyne.addKeyValuePair("vyne.example.InvoiceValue", 1000)
      runBlocking {
         val result: QueryResult = vyne.query().find("vyne.example.CreditRisk")
         result.typedInstances().first().value.should.equal(100)
         val paramsPassedToService: List<TypedInstance> = stubService.invocations["creditRisk"]!!
         expect(paramsPassedToService).size(2)
         expect(paramsPassedToService[0].value).to.equal("123")
         expect(paramsPassedToService[1].value).to.equal(1000.toBigDecimal())
      }
   }


   @Test
   @Ignore // Failing, requires investigation.
   // This test is currently failing, and needs looking into.
   // It apepars that the stubbed operation that should find the clientId from the mockService
   // isn't getting invoked when doing param discovery.
   // This happens frequently, but sometimes it does get invoked, making the test flakey.
   // The test more repeatably passes when run in isolation, using the setup code that has been
   // commented out.
   fun shouldRetrievePropertyFromService_withMultipleAttributes_whenAttributesAreDiscoverableViaGraph() {
      // Setup
//      val stubService = StubService()
//      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubService)
//      val vyne = TestSchema.vyne(queryEngineFactory)
      val (vyne, stubService) = testVyne(TestSchema.schema)

      // Given...
      val json = """
{
   "clientId" : "123",
   "name" : "Jimmy's Choos",
   "isicCode" : "retailer"
}"""
      stubService.addResponse(
         "creditRisk",
         TypedValue.from(vyne.getType("vyne.example.CreditRisk"), 100, source = Provided)
      )

      val client = vyne.parseJsonModel("vyne.example.Client", json)
      stubService.addResponse("mockClient", client)

      // We know the TaxFileNumber, which we should be able to use to discover their ClientId.
      vyne.addKeyValuePair("vyne.example.TaxFileNumber", "123")
      vyne.addKeyValuePair("vyne.example.InvoiceValue", 1000)

      //When....
      runBlocking {
         val result: QueryResult = vyne.query().find("vyne.example.CreditRisk")

         // Then....
         result.typedInstances().first().value.should.equal(100)
         val paramsPassedToService: List<TypedInstance> = stubService.invocations["creditRisk"]!!
         expect(paramsPassedToService).size(2)
         expect(paramsPassedToService[0].value).to.equal("123")
         expect(paramsPassedToService[1].value).to.equal(1000.toBigDecimal())
      }
   }


   @Test
   @Ignore
   fun given_notAllParamsOfOperationAreDiscoverable_then_methodNotInvoked() {
      TODO("Not sure what we should do here.")
   }

   @Test
   @Ignore
   // This test is disabled, as we don't really provide a way
   // in the query api to submit a 'start' node for the query.
   // Instead, we're just building a bag of facts, and then
   // asking for a result.
   // Therefore, this test would simply grab the name directly
   // from the context, which isn't what it was trying to do.
   // Not sure if we should allow a more specific API.
   fun shouldFindAPropertyValueByWalkingADirectRelationship() {

      val vyne = TestSchema.vyne()
      val client = """
         {
            "clientId" : "123",
            "name" : "Jimmy's Choos",
            "isicCode" : "retailer"
         }"""
      val invoice = """
         {
            "clientId" : "123"
         }
         """
      vyne.addJsonModel(typeName = "vyne.example.Client", json = client)

      val invoiceInstance = vyne.parseJsonModel(typeName = "vyne.example.Invoice", json = invoice)
      // The below line isn't implemented, and isn't trivial to do so, as it involves us remodelleding
      // the query api to provide an explicit start point.
      runBlocking {
         val result = vyne.from(invoiceInstance).find("vyne.example.ClientName")
         result
            .typedInstances()
            .first()
            .value
            .should.equal("Jimmy's Choos")
      }
   }

   @Test
   fun typeInheritsCanBeUsedInSingleDirectionToAssignValues() {
      val schema = """
          type Money {
            amount:String
          }
          type TradeValue inherits Money

          type alias HoldReceipt as String
          service LedgerService {
            operation holdFunds(Money):HoldReceipt
          }
      """.trimIndent()

      // In the above, I should be able to get a HoldReceipt by calling
      // holdFunds() using the TradeValue, which is a type of Money

      val (vyne, stubService) = testVyne(schema)
      val tradeValue = vyne.parseJsonModel("TradeValue", """{ "amount" : "$2.00" }""")
      stubService.addResponse("holdFunds", vyne.typedValue("HoldReceipt", "held-123"))
      runBlocking {
         val result = vyne.query(additionalFacts = setOf(tradeValue)).find("HoldReceipt")

         expect(result.isFullyResolved).to.be.`true`
         result.firstTypedInstace().value.should.equal("held-123")
      }
   }

   @Test
   fun given_targetTypeIsExposedDirectlyByService_then_canDiscoverWithNoStartPoint() {
      val schema = """
          type alias EmailAddress as String
          service CustomerService {
            operation singleEmail():EmailAddress
          }
      """.trimIndent()

      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse("singleEmail", vyne.typedValue("EmailAddress", "foo@foo.com"))

      runBlocking {
         val result = vyne.query().find("EmailAddress")

         expect(result.isFullyResolved).to.be.`true`
         result.firstTypedInstace().value.should.equal("foo@foo.com")
      }
   }

   @Test
   fun canRequestListTypeDirectlyFromService() {
      val schema = """
          type Customer {
            emails : EmailAddress[]
          }
          type alias EmailAddresses as EmailAddress[]
          type alias EmailAddress as String
          service CustomerService {
            operation emails():EmailAddress[]
          }
      """.trimIndent()

      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse("emails", vyne.typedValue("EmailAddress[]", listOf("foo@foo.com", "bar@foo.com")))

      runBlocking {
         val result = vyne.query().find("EmailAddress[]")

         expect(result.isFullyResolved).to.be.`true`
         result.rawResults.toList()
            .should.equal(listOf("foo@foo.com", "bar@foo.com"))

      }
   }

   @Test
   fun canRequestTypeAliasOfCollectionDirectlyFromService() {
      val schema = """
          type Customer {
            emails : EmailAddress[]
          }
          type alias EmailAddresses as EmailAddress[]
          type alias EmailAddress as String
          service CustomerService {
            operation emails():EmailAddresses
          }
      """.trimIndent()

      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse("emails", vyne.typedValue("EmailAddresses", listOf("foo@foo.com", "bar@foo.com")))

      runBlocking {

         val resultsFromEmailAddresses = vyne.query().find("EmailAddresses")
         expect(resultsFromEmailAddresses.isFullyResolved).to.be.`true`
         resultsFromEmailAddresses.rawResults.toList()
            .should.equal(listOf("foo@foo.com", "bar@foo.com"))

         // Discovery by the aliases type name should work too
         val resultFromAliasName = vyne.query().find("EmailAddress[]")
         expect(resultFromAliasName.isFullyResolved).to.be.`true`
         resultFromAliasName.rawResults.toList()
            .should.equal(
               listOf(
                  "foo@foo.com",
                  "bar@foo.com"
               )
            )
      }
   }

   @Test
   fun canDiscoverAliasedTypesWhenUsingGraphDiscoveryStrategy() {
      val schema = """
          type Customer {
            name : CustomerName as String
            emails : EmailAddresses
          }
          type alias EmailAddresses as EmailAddress[]
          type alias EmailAddress as String
          type alias Region as String
          type alias CustomerList as Customer[]
          service CustomerService {
            operation customersInRegion(Region):CustomerList
          }
      """.trimIndent()

      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse(
         "customersInRegion", vyne.addJsonModel(
            "CustomerList", """
         [
            { "name" : "Jimmy", "emails" : [ "foo@foo.com" ] },
            { "name" : "Jack", "emails" : [ "baz@foo.com" ] }
         ]
         """.trimIndent()
         )
      )
      runBlocking {
         listOf("CustomerList", "Customer[]").forEach { typeToDiscover ->
            val result =
               vyne.query(additionalFacts = setOf(vyne.typedValue("Region", "UK")))
                  .find(typeToDiscover)
                  .typedObjects()
            result.should.have.size(2)
         }
      }
   }

   @Test
   fun canInvokeServiceWithParamMatchingOnType() {
      val schema = """
         type Pet {
            id : Int
         }
         service PetService {
            @HttpOperation(method = "GET" , url = "http://petstore.swagger.io/api/pets/{id}")
            operation findPetById(  id : Int ) : Pet
         }
      """.trimIndent()

      runBlocking {
         val (vyne, stubService) = testVyne(schema)
         stubService.addResponse("findPetById", vyne.typedValue("Pet", mapOf("id" to 100)))
         vyne.addKeyValuePair("lang.taxi.Int", 100)
         val result = vyne.query().find("Pet").results.toList()

         //expect(result.isFullyResolved).to.be.`true`
         val params = stubService.invocations["findPetById"]!!.get(0)
         expect(params.value).to.equal(100)
      }
   }

   @Test
   @Ignore("This feature isn't built yet")
   fun given_xmlBlobWithSchema_then_canAccessValeus() {
      val src = """
type Money {
   amount : MoneyAmount as Decimal
   currency : Currency as String
}
type alias Instrument as String
type NearLegNotional inherits Money {}
type FarLegNotional inherits Money {}

type LegacyTradeNotification {
   nearLegNotional : NearLegNotional {
       amount by xpath("/tradeNotification/legs/leg[1]/notional/amount/text()")
       currency by xpath("/tradeNotification/legs/leg[1]/currency/amount/text()")
   }
   farLegNotional : FarLegNotional {
       amount by xpath("/tradeNotification/legs/leg[2]/notional/amount/text()")
       currency by xpath("/tradeNotification/legs/leg[2]/currency/amount/text()")
   }
}
        """.trimIndent()
      val (vyne, stubService) = testVyne(src)
      val xml = """
 <tradeNotification>
    <legs>
        <leg>
            <notional>
                <amount>200000</amount>
                <currency>GBP</currency>
            </notional>
        </leg>
        <leg>
            <notional>
                <amount>700000</amount>
                <currency>GBP</currency>
            </notional>
        </leg>
    </legs>
</tradeNotification>
      """.trimIndent()
      val instance =
         TypedInstance.from(vyne.schema.type("LegacyTradeNotification"), xml, vyne.schema, source = Provided)
      vyne.addModel(instance)
      val queryResult = runBlocking { vyne.query().find("NearLegNotional") }
      TODO()
   }

   val schema = """
type alias OrderDate as Date
type alias OrderId as String

model CommonOrder {
   id: OrderId
   date: OrderDate
}

model Order {
}
type Broker1Order inherits Order {
   broker1ID: OrderId
   broker1Date: OrderDate
}
type Broker2Order inherits Order {
   broker2ID: OrderId
   broker2Date: OrderDate
}

// operations
service Broker1Service {
   operation getAllBroker1Orders() : Broker1Order[]
   operation getBroker1Orders( start : OrderDate, end : OrderDate) : Broker1Order[] (OrderDate >= start, OrderDate < end)
}
service Broker2Service {
   operation getAllBroker2Orders() : Broker2Order[]
   operation getBroker2Orders( start : OrderDate, end : OrderDate) : Broker2Order[] (OrderDate >= start, OrderDate < end)
}

""".trimIndent()

   @Test
   @Ignore("This doesn't pass any criteria, which is resulting in services that expose criteria not getting invoked.  Not sure what the expected behaviour should be, will revisit")
   fun canGatherOrdersFromTwoDifferentServices() {
      // prepare
      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse(
         "getBroker1Orders", vyne.parseJsonModel(
            "Broker1Order[]", """
         [
            { "broker1ID" : "Broker1Order1", "broker1Date" : "2020-01-01"}
         ]
         """.trimIndent()
         )
      )
      stubService.addResponse(
         "getBroker2Orders", vyne.parseJsonModel(
            "Broker2Order[]", """
         [
            { "broker2ID" : "Broker2Order1", "broker2Date" : "2020-01-01"}
         ]
         """.trimIndent()
         )
      )

      // act
      runBlocking {
         val result = vyne.query().findAll("Order[]")

         // assert
         expect(result.isFullyResolved).to.be.`true`
         val resultList = result.rawObjects()
         resultList.should.contain.all.elements(
            mapOf("broker1ID" to "Broker1Order1", "broker1Date" to LocalDate.parse("2020-01-01")),
            mapOf("broker2ID" to "Broker2Order1", "broker2Date" to LocalDate.parse("2020-01-01"))
         )
      }
   }

   @Test
   fun canGatherOrdersFromTwoDifferentServices_AndFilterByDateRange() {
      // prepare
      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse(
         "getBroker1Orders", vyne.parseJsonModel(
            "Broker1Order[]", """
         [
            { "broker1ID" : "Broker1Order1", "broker1Date" : "2020-01-01"},
            { "broker1ID" : "Broker1Order2", "broker1Date" : "2020-01-02"}
         ]
         """.trimIndent()
         )
      )
      stubService.addResponse(
         "getBroker2Orders", vyne.parseJsonModel(
            "Broker2Order[]", """
         [
            { "broker2ID" : "Broker2Order1", "broker2Date" : "2020-01-01"},
            { "broker2ID" : "Broker2Order2", "broker2Date" : "2020-01-02"}
         ]
         """.trimIndent()
         )
      )

      // act
      runBlocking {
         val result = vyne.query("""findAll { Order[]( OrderDate >= "2020-01-01" , OrderDate < "2020-01-02" ) }""")
         val resultList = result.rawResults.toList()
         resultList.should.have.size(4)
         stubService.invocations.should.have.size(2)
      }
   }


   @Test
   fun canProjectDifferentOrderTypesToSingleType() {
      // prepare
      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse(
         "getAllBroker1Orders", vyne.parseJsonCollection(
            "Broker1Order[]", """
         [
            { "broker1ID" : "Broker1Order1", "broker1Date" : "2020-01-01"}
         ]
         """.trimIndent()
         )
      )
      stubService.addResponse(
         "getAllBroker2Orders", vyne.parseJsonCollection(
            "Broker2Order[]", """
         [
            { "broker2ID" : "Broker2Order1", "broker2Date" : "2020-01-01"}
         ]
         """.trimIndent()
         )
      )

      // act
      runBlocking {
         val result = vyne.query(
            """
         findAll { Order[] } as CommonOrder[]
      """.trimIndent()
         )

         // assert
         expect(result.isFullyResolved).to.be.`true`
         result.rawObjects().should.contain.all.elements(
            mapOf("id" to "Broker1Order1", "date" to "2020-01-01"),
            mapOf("id" to "Broker2Order1", "date" to "2020-01-01")
         )
      }
   }

   @Test
   fun canProjectDifferentOrderTypesToSingleTypeFromUsingVyneQLQuery() {
      // prepare
      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse(
         "getAllBroker1Orders", vyne.parseJsonCollection(
            "Broker1Order[]", """
         [
            { "broker1ID" : "Broker1Order1", "broker1Date" : "2020-01-01"}
         ]
         """.trimIndent()
         )
      )
      stubService.addResponse(
         "getAllBroker2Orders", vyne.parseJsonCollection(
            "Broker2Order[]", """
         [
            { "broker2ID" : "Broker2Order1", "broker2Date" : "2020-01-01"}
         ]
         """.trimIndent()
         )
      )

      // act
      runBlocking {
         val result =
            vyne.query(
               """
            findAll { Order[] } as CommonOrder[]
         """.trimIndent()
            )

         // assert
         expect(result.isFullyResolved).to.be.`true`
         val resultList = result.rawObjects().should.contain.all.elements(
            mapOf("id" to "Broker1Order1", "date" to "2020-01-01"),
            mapOf("id" to "Broker2Order1", "date" to "2020-01-01")
         )
      }
   }

   @Test
   fun canProjectDifferentOrderTypesToSingleType_whenSomeValuesAreMissing() {
      // prepare
      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse(
         "getAllBroker1Orders", vyne.parseJsonModel(
            "Broker1Order[]", """
         [
            { "broker1ID" : "Broker1Order1"}
         ]
         """.trimIndent()
         )
      )
      stubService.addResponse(
         "getAllBroker2Orders", vyne.parseJsonModel(
            "Broker2Order[]", """
         [
            { "broker2ID" : "Broker2Order1", "broker2Date" : "2020-01-01"}
         ]
         """.trimIndent()
         )
      )

      // act
      runBlocking {
         val result = vyne.query().findAll("Order[]")
         val collected = result.rawResults.toList()

         collected.should.contain.all.elements(
            mapOf("broker1ID" to "Broker1Order1"),
            mapOf("broker2ID" to "Broker2Order1", "broker2Date" to "2020-01-01")
         )
      }
   }

   @Test
   fun formattedValueWithSameTypeButDifferentFormatsAreDiscoverable() {
      val schema = """
         type EventDate inherits Instant
         model Source {
            eventDate : EventDate( @format = "MM/dd/yy'T'HH:mm:ss.SSSX" )
         }
         model Target {
            eventDate : EventDate( @format = "yyyy-MM-dd'T'HH:mm:ss.SSSX" )
         }
      """.trimIndent()
      val (vyne, _) = testVyne(schema)
      vyne.addJsonModel("Source", """{ "eventDate" : "05/28/20T13:44:23.000Z" }""")
      runBlocking {
         val result = vyne.query().build("Target")
         result.isFullyResolved.should.be.`true`
         result.firstRawObject().get("eventDate").should.equal("2020-05-28T13:44:23.000Z")
      }
   }

   @Test
   fun `when projecting, values with same type but different format get formats applied`() {
      val schema = """
         type EventDate inherits Instant
         model Source {
            eventDate : EventDate( @format = "MM/dd/yy'T'HH:mm:ss.SSSX" )
         }
         model Target {
            eventDate : EventDate( @format = "MM-dd-yy'T'HH:mm:ss.SSSX" )
         }
      """.trimIndent()
      val (vyne, _) = testVyne(schema)
      vyne.addJsonModel("Source", """{ "eventDate" : "05/28/20T13:44:23.000Z" }""")
      runBlocking {
         val result = vyne.query("""findOne { Source } as Target""")
         result.isFullyResolved.should.be.`true`
         result.firstRawObject().get("eventDate").should.equal("05-28-20T13:44:23.000Z")
      }
   }

   @Test
   fun `when projecting a collection, values with same type but different format get formats applied`() {
      val schema = """
         type EventDate inherits Instant
         model Source {
            eventDate : EventDate( @format = "MM/dd/yy'T'HH:mm:ss.SSSX" )
         }
         model Target {
            eventDate : EventDate( @format = "MM-dd-yy'T'HH:mm:ss.SSSX" )
         }
      """.trimIndent()
      val (vyne, _) = testVyne(schema)
      vyne.addJsonModel("Source[]", """[{ "eventDate" : "05/28/20T13:44:23.000Z" }]""")

      runBlocking {

         val result = vyne.query("""findOne { Source[] } as Target[]""")
         result.isFullyResolved.should.be.`true`
         result.rawObjects().first()
            .get("eventDate").should.equal("05-28-20T13:44:23.000Z")
      }
   }

   @Ignore("This test throws StackOverFlowException, will be investigated.")
   @Test
   fun `should use cache for multiple invocations of given service operation`() {
      val testSchema = """
         model Client {
            name : PersonName as String
            country : CountryCode as String
         }
         model Country {
             countryCode : CountryCode
             countryName : CountryName as String
         }
         model ClientAndCountry {
            personName : PersonName
            countryName : CountryName
         }

         service MultipleInvocationService {
            @StubResponse("mockCustomers")
            operation getCustomers():Client[]

            @StubResponse("mockCountry")
            operation getCountry(CountryCode): Country
         }
      """.trimIndent()
      val stubInvocationService = StubService()

      val cacheAwareInvocationService = CacheAwareOperationInvocationDecorator(stubInvocationService)
      val queryEngineFactory =
         QueryEngineFactory.withOperationInvokers(VyneCacheConfiguration.default(), emptyList(), cacheAwareInvocationService)
      val vyne = Vyne(queryEngineFactory).addSchema(TaxiSchema.from(testSchema))
      stubInvocationService.addResponse(
         "mockCustomers", vyne.parseJsonModel(
            "Client[]", """
         [
            { name : "Jimmy", country : "UK" },
            { name : "Marty", country : "UK" },
            { name : "Devrim", country : "TR" }
         ]
         """.trimIndent()
         )
      )


      stubInvocationService.addResponse("mockCountry") { _, parameters ->
         val countryCode = parameters.first().second.value!!.toString()
         if (countryCode == "UK") {
            listOf(vyne.typedValue("Country", "United Kingdom"))
         } else {
            listOf(vyne.typedValue("Country", "Turkey"))
         }
      }
//      val result =  vyne.query("""
//        findAll { Client } as ClientAndCountry
//      """.trimIndent())
   }

   val enumSchema = TaxiSchema.from(
      """
                namespace common {
                   enum BankDirection {
                     BankBuys("bankbuys"),
                     BankSells("banksell")
                   }

                   model CommonOrder {
                      direction: BankDirection
                   }
                }
                namespace BankX {
                   enum BankXDirection {
                        BUY("buy") synonym of common.BankDirection.BankBuys,
                        SELL("sell") synonym of common.BankDirection.BankSells
                   }
                   model BankOrder {
                      buySellIndicator: BankXDirection
                   }
                }

      """.trimIndent()
   )

   @Test
   fun `should build by using synonyms`() {

      // Given
      val (vyne, stubService) = testVyne(enumSchema)
      vyne.addJson(
         "BankX.BankOrder", """ { "buySellIndicator" : "BUY" } """
      )

      runBlocking {
         // When
         val result = vyne.query().build("common.CommonOrder")

         // Then
         val rawResult = result.rawObjects().first().should.equal(mapOf("direction" to "BankBuys"))
      }

   }

   @Test
   fun `should build by using lenient synonyms`() {
      val lenientEnumSchema = TaxiSchema.from(
         """
                namespace common {
                   enum BankDirection {
                     BankBuys("bankbuys"),
                     BankSell("banksell")
                   }

                   model CommonOrder {
                      direction: BankDirection
                   }
                }
                namespace BankX {
                   lenient enum BankXDirection {
                        BUY("bank_buys") synonym of common.BankDirection.BankBuys,
                        SELL("bank_sells") synonym of common.BankDirection.BankSell
                   }
                   model BankOrder {
                      buySellIndicator: BankXDirection
                   }
                }

      """.trimIndent()
      )

      // Given
      val (vyne, stubService) = testVyne(lenientEnumSchema)

      suspend fun query(factJson: String): TypedObject {
         return vyne
            .query(
               additionalFacts = setOf(
                  vyne.parseJson("BankX.BankOrder", factJson)
               )
            )
            .build("common.CommonOrder")
            .firstTypedObject()
      }
      // When
      runBlocking {
         query(""" { "buySellIndicator" : "BUY" } """)["direction"].value.should.equal("BankBuys")
         query(""" { "buySellIndicator" : "buy" } """)["direction"].value.should.equal("BankBuys")
      }
   }

   @Test
   fun `should build by using default enum values`() {
      val lenientEnumSchema = TaxiSchema.from(
         """
                namespace common {
                   enum BankDirection {
                     BankBuys("bankbuys"),
                     BankSell("banksells")
                   }

                   model CommonOrder {
                      direction: BankDirection
                   }
                }
                namespace BankX {
                   enum BankXDirection {
                        BUY("bank_buys") synonym of common.BankDirection.BankBuys,
                        default SELL("bank_sells") synonym of common.BankDirection.BankSell
                   }
                   model BankOrder {
                      buySellIndicator: BankXDirection
                   }
                }

      """.trimIndent()
      )

      // Given
      val (vyne, stubService) = testVyne(lenientEnumSchema)

      suspend fun query(factJson: String): TypedObject {
         return vyne
            .query(
               additionalFacts = setOf(
                  vyne.parseJson("BankX.BankOrder", factJson)
               )
            )
            .build("common.CommonOrder")
            .firstTypedObject()
      }
      // When
      runBlocking {
         // BUY is the enum name, so should map to BankBuys, the enum name of the corresponding synonym
         query(""" { "buySellIndicator" : "BUY" } """)["direction"].value.should.equal("BankBuys")
         // Note here that badValue doesn't resolve, so the default of SELL should be applied.
         // Defaults always use names, not values.
         query(""" { "buySellIndicator" : "badValue" } """)["direction"].value.should.equal("BankSell")
      }
   }

   @Test
   fun `should build by using synonyms with vyneql`() {

      // Given
      val (vyne, stubService) = testVyne(enumSchema)
      vyne.addJson(
         "BankX.BankOrder[]",
         """ [ { "buySellIndicator" : "BUY" }, { "buySellIndicator" : "SELL" } ] """.trimIndent()
      )

      // When
      runBlocking {
         val result = vyne.query(""" findOne { BankOrder[] } as  CommonOrder[] """)

         // Then
         val results = result.rawObjects()
         results.size.should.equal(2)
         // MP : Check this - I think I changed the test a little bit
         results.should.equal(
            listOf(mapOf("direction" to "BankBuys"), mapOf("direction" to "BankSells"))
         )
      }
   }

   @Test
   fun `should build by using synonyms value and name`() {

      val (vyne, stubService) = testVyne(enumSchema)

      runBlocking {
         // Query by enum value
         val factValue = TypedInstance.from(vyne.type("BankDirection"), "bankbuys", vyne.schema)
         val resultValue = vyne.query(additionalFacts = setOf(factValue)).build("BankOrder")
         resultValue.rawObjects().first().should.equal(mapOf("buySellIndicator" to "buy"))

         // Query by enum name
         val factName = TypedInstance.from(vyne.type("BankDirection"), "BankSells", vyne.schema)
         val resultName = vyne.query(additionalFacts = setOf(factName)).build("BankOrder")
         resultName.rawObjects().first().should.equal(mapOf("buySellIndicator" to "SELL"))
      }
   }

   @Test
   fun `should build by using synonyms value and name different than String`() {

      val enumSchema = TaxiSchema.from(
         """
                namespace common {
                   enum BankDirection {
                     BankBuys(1),
                     BankSell(2)
                   }

                   model CommonOrder {
                      direction: BankDirection
                   }
                }
                namespace BankX {
                   enum BankXDirection {
                        BUY(3) synonym of common.BankDirection.BankBuys,
                        SELL(4) synonym of common.BankDirection.BankSell
                   }
                   model BankOrder {
                      buySellIndicator: BankXDirection
                   }
                }

      """.trimIndent()
      )

      val (vyne, stubService) = testVyne(enumSchema)
      vyne.addJson(
         "BankX.BankOrder", """ { "buySellIndicator" : 3 } """
      )

      // When
      runBlocking {
         val result = vyne.query().build("common.CommonOrder")

         // Then
         val rawResult = result.rawObjects().first().should.equal(mapOf("direction" to 1))
      }
   }


   @Test
   fun `retrieve all types that can discovered through single argument function invocations`() {
      val testSchema = """
namespace vyne.example
type Invoice {
   clientId : ClientId
   invoiceValue : InvoiceValue as Decimal
}
type Client {
   clientId : ClientId as String
   name : ClientName as String
   isicCode : IsicCode as String
}
type alias TaxFileNumber as String
type alias CreditRisk as Int
type alias NaicsCode as Int

service ClientService {
   @StubResponse("mockClient")
   operation getClient(TaxFileNumber):Client

   @StubResponse("creditRisk")
   operation getCreditRisk(ClientId,InvoiceValue):CreditRisk

   @StubResponse("mockClients")
   operation getClients(NaicsCode):Client[]

   operation getClients(Client):Invoice
}
""".trimIndent()
      val stubInvocationService = StubService()
      val queryEngineFactory =
         QueryEngineFactory.withOperationInvokers(VyneCacheConfiguration.default(), emptyList(), stubInvocationService)
      val vyne = Vyne(queryEngineFactory).addSchema(TaxiSchema.from(testSchema))
      val fqn = "vyne.example.TaxFileNumber"
      val accessibleTypes = vyne.accessibleFrom(fqn)
      accessibleTypes.should.have.size(2)
   }

   @Test
   fun `retrieve all types that can discovered through single argument function invocations in a large graph`() =
      runBlockingTest {
         val schemaBuilder = StringBuilder()
            .appendLine("namespace vyne.example")

         val end = 1000
         val range = 0..end

         for (index in range) {
            schemaBuilder.appendLine("type alias Type$index as String")
         }

         schemaBuilder.appendLine("service serviceWithTooManyOperations {")
         for (index in 0 until range.last) {
            schemaBuilder.appendLine("operation getType$index(Type$index): Type${index + 1}")
         }
         schemaBuilder.appendLine("}")

         val stubInvocationService = StubService()
         val queryEngineFactory =
            QueryEngineFactory.withOperationInvokers(VyneCacheConfiguration.default(), emptyList(), stubInvocationService)
         val vyne = Vyne(queryEngineFactory).addSchema(TaxiSchema.from(schemaBuilder.toString()))

         val fqn = "vyne.example.Type0"
         val accessibleTypes = vyne.accessibleFrom(fqn)
         accessibleTypes.should.have.size(end)
      }

   @Test
   fun `given multiple valid services to call with equal cost, should invoke all until a result is found`() {
      // The issue we're testing here is if there are mutliple ways to find a value, all which look the same,
      // but some which generate values, and others that don't, that we should keep trying until we find the approach
      // that works.
      val (vyne, stub) = testVyne(
         """
         model User {
            userId : UserId as Int
            userName : UserName as String
         }
         service Users {
            @StubResponse("lookupByIdEven")
            operation lookupByIdEven(id:UserId):User
            @StubResponse("lookupByIdOdd")
            operation lookupByIdOdd(id:UserId):User
         }
      """.trimIndent()
      )

      stub.addResponse("lookupByIdEven") { _, parameters ->
         val (_, userId) = parameters.first()
         val userIdValue = userId.value as Int
         if (userIdValue % 2 == 0) {
            listOf(vyne.parseJsonModel("User", """{ "userId" : $userIdValue, "userName" : "Jimmy Even" }"""))
         } else {
            error("Not found") // SImulate a 404
//            TypedNull(vyne.type("User"))
         }
      }
      stub.addResponse("lookupByIdOdd") { _, parameters ->
         val (_, userId) = parameters.first()
         val userIdValue = userId.value as Int
         if (userIdValue % 2 != 0) {
            listOf(vyne.parseJsonModel("User", """{ "userId" : $userIdValue, "userName" : "Jimmy Odd" }"""))
         } else {
            error("not found")  // SImulate a 404
//            TypedNull(vyne.type("User"))
         }
      }

      runBlocking {
         val resultEven =
            vyne.query(additionalFacts = setOf(vyne.parseKeyValuePair("UserId", 2))).find("UserName")
         resultEven.isFullyResolved.should.be.`true`
         resultEven.firstTypedInstace().value.should.equal("Jimmy Even")

         val resultOdd =
            vyne.query(additionalFacts = setOf(vyne.parseKeyValuePair("UserId", 3))).find("UserName")
         resultOdd.isFullyResolved.should.be.`true`
         resultOdd.firstTypedInstace().value.should.equal("Jimmy Odd")
      }
   }

   @Test
   fun `when projecting to a model, functions on the output model are evaluated`() {
      val (vyne, stub) = testVyne(
         """
         model OutputModel {
            username : String by concat(this.firstName,this.lastName)
            firstName : FirstName as String
            lastName : LastName as String
            favouriteCoffee : String by default("Latte")
         }
         model InputModel {
            firstName : FirstName
            lastName : LastName
         }
         service UserService {
            @StubResponse("findUsers")
            operation findAllUsers() : InputModel[]
         }
      """
      )
      val inputJson = """{ "firstName" : "Jimmy", "lastName" : "Pitt" }"""
      val user = TypedInstance.from(vyne.type("InputModel"), inputJson, vyne.schema, source = Provided)
      stub.addResponse("findUsers", TypedCollection.from(listOf(user)))

      runBlocking {
         val queryResult = vyne.query("findAll { InputModel[] } as OutputModel[]")

         val firstEntity = queryResult.typedObjects().first()
         firstEntity["username"].value.should.equal("JimmyPitt")
         firstEntity["favouriteCoffee"].value.should.equal("Latte")
         firstEntity["firstName"].value.should.equal("Jimmy")
         firstEntity["lastName"].value.should.equal("Pitt")
      }
   }

   @Test
   fun `testing nulls on responses - when service returns object that is partially populated then the populated values are used for discovery`() {
      val (vyne, stub) = testVyne(
         """
         type FirstName inherits String
         type LastName inherits String
         type PersonId inherits Int
         type PersonAge inherits Int
         type EmailAddress inherits String
         model Person {
            personId : PersonId
            firstName : FirstName
            lastName : LastName
            personAge : PersonAge
         }
         model OutputModel {
            @FirstNotEmpty
            personId : PersonId
            @FirstNotEmpty
            firstName : FirstName
            @FirstNotEmpty
            lastName : LastName
            @FirstNotEmpty
            personAge : PersonAge
         }
         model PersonDetails {
          firstName : FirstName
             lastName : LastName
            personAge : PersonAge
         }
         service PersonService {
            operation findPersonId(EmailAddress):PersonId
            operation findPerson(PersonId):Person
            operation findPersonDetails(PersonId):PersonDetails
         }
      """.trimIndent()
      )
      stub.addResponse("findPersonId", vyne.parseKeyValuePair("PersonId", 1))
      stub.addResponse(
         "findPersonDetails", vyne.parseJsonModel(
            "Person", """{
         | "firstName" : null,
         | "lastName" : "Foo",
         | "personAge" : null
         | }
      """.trimMargin()
         )
      )
      stub.addResponse(
         "findPerson", vyne.parseJsonModel(
            "Person", """{
         | "personId" : 1,
         | "firstName" : "Jimmy",
         | "lastName" : null,
         | "personAge" : 23
         | }
      """.trimMargin()
         )
      )

      runBlocking {
         val queryResult =
            vyne.query("""given { email : EmailAddress = "jimmy@demo.com" } findOne { Person } as OutputModel""")
         queryResult.rawObjects().first().should.equal(
            mapOf(
               "personId" to 1,
               "firstName" to "Jimmy",
               "lastName" to "Foo",
               "personAge" to 23
            )
         )
      }
   }

   @Test
   fun `vyne should accept Instant parameters that are in ISO format`() {
      val testSchema = """
         type alias Symbol as String
         type TransactionEventDateTime inherits Instant

         type OrderWindowSummary {
            symbol : Symbol by xpath("/symbol")
            // 2019-12-03 16:07:59.7980000
            @Between
            orderDateTime : TransactionEventDateTime( @format = "yyyy-MM-dd HH:mm:ss.SSSSSSS") by xpath("/eventDate")
         }

         service CacheService {
            @StubResponse("findBetween")
            operation findByOrderDateTimeBetween(start : TransactionEventDateTime, end : TransactionEventDateTime ):
                       OrderWindowSummary[]( TransactionEventDateTime >= start, TransactionEventDateTime < end )
         }
      """.trimIndent()
      val stubInvocationService = StubService()
      val queryEngineFactory =
         QueryEngineFactory.withOperationInvokers(VyneCacheConfiguration.default(), emptyList(), stubInvocationService)
      val vyne = Vyne(queryEngineFactory).addSchema(TaxiSchema.from(testSchema))
      stubInvocationService.addResponse("findBetween") { _, parameters ->
         parameters.should.have.size(2)
         parameters[0].second.value.should.be.equal(Instant.parse("2011-12-03T10:15:30Z"))
         parameters[1].second.value.should.be.equal(Instant.parse("2021-12-03T10:15:30Z"))
         vyne.parseJsonCollection(
            "OrderWindowSummary[]",
            """
                  [
                    {
                         "symbol": "USD",
                         "orderDateTime": "2019-12-03 13:07:59.7980000"
                     }
                  ]
               """.trimIndent()
         )
      }

      runBlocking {
         vyne.query(
            """
              findAll {
                 OrderWindowSummary[] ( TransactionEventDateTime  >= "2011-12-03T10:15:30", TransactionEventDateTime < "2021-12-03T10:15:30" )
              }
              """.trimIndent()
         )
      }
   }

   @Test
   fun `if query processing throws exception on attribute query should continue and use null for value`() {
      val (vyne, stubs) = testVyne(
         """
            type Quantity inherits Int
            type Price inherits Int

            model Order {
               quantity : Quantity
               price : Price
            }
            model Output {
               quantity : Quantity
               price : Price
               averagePrice : Decimal by (this.price / this.quantity)
            }
            service OrderService {
               operation listOrders():Order[]
            }
         """.trimIndent()
      )
      // The below responseJson will trigger a divide-by-zero
      val responseJson = """[
         |{ "quantity" : 0 , "price" : 2 }
         |]""".trimMargin()
      stubs.addResponse(
         "listOrders", vyne.parseJsonModel(
            "Order[]", """[
         |{ "quantity" : 0 , "price" : 2 }
         |]""".trimMargin()
         )
      )
      runBlocking {
         val queryResult = vyne.query("findAll { Order[] } as Output[]")

         val outputModel = queryResult.typedObjects().first()
         outputModel["averagePrice"].value.should.be.`null`
         val source = outputModel["averagePrice"].source
         require(source is FailedEvaluatedExpression)
         source.expressionTaxi.should.equal("this.price / this.quantity")
         source.errorMessage.should.equal("BigInteger divide by zero")
      }
   }

   @Test
   fun `data integration with inheritance`() {
      val (vyne, stubs) = testVyne(
         """
         namespace Foo {
           type Isin inherits String
         }

         namespace Bar {
            type PUID inherits String
            type Isin inherits String
            type ProductIsin inherits Isin
            type InstrumentIsin inherits ProductIsin

            parameter model PuidRequest {
                isin: Isin
            }

            model PuidResponse {
                puid: PUID
            }

            service ProductService {
               operation getPUID(PuidRequest) :  PuidResponse
            }
         }
      """.trimIndent()
      )

      stubs.addResponse("getPUID") { operation, parameters ->
         val isinArgValue = parameters.first().second.value as Map<String, TypedValue>
         val response = """{
         |"puid": "${isinArgValue["isin"]?.value.toString()}"
         |}
          """.trimMargin()
         listOf(TypedInstance.from(vyne.type("PuidResponse"), response, vyne.schema, source = Provided))
      }

      runBlocking {
         val queryResult1 =
            vyne.query(
               """
         given {
            isin: Bar.ProductIsin = "US500769FH22"
         } findOne {
            PuidResponse
         }
      """.trimIndent()
            )
         queryResult1.isFullyResolved.should.be.`true`
         val puidResponse1 = queryResult1.firstTypedObject()
         puidResponse1["puid"].value.should.equal("US500769FH22")

         val queryResult2 =
            vyne.query(
               """
         given {
            isin: Bar.InstrumentIsin = "US500769FH23"
         } findOne {
            PuidResponse
         }
      """.trimIndent()
            )
         queryResult2.isFullyResolved.should.be.`true`
         val puidResponse2 = queryResult2.firstTypedObject()
         puidResponse2["puid"].value.should.equal("US500769FH23")

         val queryResult3 =
            vyne.query(
               """
         given {
            isin: Bar.Isin = "US500769FH24"
         } findOne {
            PuidResponse
         }
      """.trimIndent()
            )
         queryResult3.isFullyResolved.should.be.`true`
         val puidResponse3 = queryResult3.firstTypedObject()
         puidResponse3["puid"].value.should.equal("US500769FH24")
      }

   }

   @Test
   fun `parameter models should be resolved by respecting nullability attributes of its fields`() {
      val (vyne, stubs) = testVyne(
         """
         type Isin inherits String
         type PUID inherits String
         type InstrumentId inherits String
         parameter model PuidRequest {
            //note that isin is not nullable
            isin: Isin
         }
         model PuidResponse {
            puid: PUID
         }

         model Instrument {
           id: InstrumentId
           isin: Isin
         }

         service ProductService {
           operation getPUID(PuidRequest) :  PuidResponse
         }

         service instrumentService {
            operation getInstrument(InstrumentId): Instrument
         }

      """.trimIndent()
      )

      stubs.addResponse("getPUID") { _, _ -> fail("getPUID should not be called") }
      stubs.addResponse(
         "getInstrument",
         TypedInstance.from(
            vyne.type("Instrument"), """
            "id": "instrument1"
         """.trimIndent(), vyne.schema, source = Provided
         )
      )

      runBlocking {
         val queryResult1 = vyne.query(
            """
          given { id: InstrumentId = "1" }
          findOne {
            PuidResponse
         }
      """.trimIndent()
         )
      }

   }

   @Test
   fun `GATHER strategy should respect the constraints in Query`() {
      val (vyne, stubs) = testVyne(
         """
         namespace Bar {
            type Isin inherits String
            model Order {
               isin: Isin
            }

            service OrderService {
              operation `findAll`(): Order[]
              operation findOrder(): Order
            }
         }
      """.trimIndent()
      )
      stubs.addResponse("`findAll`") { _, _ ->
         fail("should not call findAll")
      }

      stubs.addResponse("findOrder") { _, _ ->
         fail("should not call findOrder")
      }

      runBlocking {
         vyne.query(
            """
            findAll {
    Bar.Order[](Isin== 'IT0000312312')
    }
      """.trimIndent()
         )
      }

   }

   @Test
   @Ignore("not yet implemented")
   fun `can use a derived field as an input for discovery`() {
      val (vyne, stub) = testVyne(
         """
         type Name inherits String
         type FirstName inherits Name
         type NickName inherits Name
         type UserName inherits Name
         type Age inherits Int
         service NameService {
            operation findAgeByName(UserName):Age
         }
         model InputModel {
            firstName : FirstName?
            nickName : NickName?
         }
         model OutputModel {
            firstName : FirstName?
            nickName : NickName?

            userName : UserName by when {
               this.firstName != null -> firstName
               else -> nickName
            }

            age : Age
         }
         """
      )
      stub.addResponse("findAgeByName", vyne.typedValue("Age", 28))

      runBlocking {
         val result =
            vyne.from(vyne.parseJsonModel("InputModel", """{ "firstName" : "jimmy" , "nickName" : "J-Dawg" }"""))
               .build("OutputModel")
      }
      TODO()
   }


}

fun Vyne.typedValue(typeName: String, value: Any, source: DataSource = Provided): TypedInstance {
   return TypedInstance.from(this.getType(typeName), value, this.schema, source = source)
//   return TypedValue.from(this.getType(typeName), value)
}


data class Edge(val operation: Operation)
data class Vertex(val type: Type)
