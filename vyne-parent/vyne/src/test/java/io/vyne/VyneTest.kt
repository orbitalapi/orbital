package io.vyne

import com.winterbe.expekt.expect
import io.vyne.models.TypedInstance
import io.vyne.models.TypedValue
import io.vyne.models.json.addJsonModel
import io.vyne.models.json.addKeyValuePair
import io.vyne.models.json.parseJsonModel
import io.vyne.query.*
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Ignore
import org.junit.Test

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

service ClientService {
   @StubResponse("mockClient")
   operation getClient(TaxFileNumber):Client

   @StubResponse("creditRisk")
   operation getCreditRisk(ClientId,InvoiceValue):CreditRisk
}
"""
   val schema = TaxiSchema.from(taxiDef)


   fun vyne(queryEngineFactory: QueryEngineFactory = QueryEngineFactory.default()) = Vyne(queryEngineFactory).addSchema(schema)
   val queryParser = QueryParser(schema)

   fun typeNode(name: String): Set<QuerySpecTypeNode> {
      return queryParser.parse(TypeNameQueryExpression(name))
   }

   fun queryContext(): QueryContext = vyne().queryEngine().queryContext()
}

fun testVyne(schema: TaxiSchema): Pair<Vyne, StubService> {
   val stubService = StubService()
   val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubService)
   val vyne = Vyne(queryEngineFactory).addSchema(schema)
   return vyne to stubService
}

fun testVyne(schema: String) = testVyne(TaxiSchema.from(schema))

class VyneTest {


   @Test
   fun shouldFindAPropertyOnAnObject() {

      val vyne = TestSchema.vyne()
      val json = """
{
   "clientId" : "123",
   "name" : "Jimmy's Choos",
   "isicCode" : "retailer"
}"""
      vyne.addJsonModel("vyne.example.Client", json)
      val result = vyne.query().find("vyne.example.ClientName")
      expect(result.results.size).to.equal(1)
      expect(result["vyne.example.ClientName"]!!.value).to.equal("Jimmy's Choos")
   }

   @Test
   fun shouldRetrievePropertyFromService() {
      val stubService = StubService()
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubService)
      val vyne = TestSchema.vyne(queryEngineFactory)

      val json = """
{
   "clientId" : "123",
   "name" : "Jimmy's Choos",
   "isicCode" : "retailer"
}"""
      val client = vyne.parseJsonModel("vyne.example.Client", json)
      stubService.addResponse("mockClient", client)
      vyne.addKeyValuePair("vyne.example.TaxFileNumber", "123")
      val result: QueryResult = vyne.query().find("vyne.example.ClientName")
      expect(result.results.size).to.equal(1)
      expect(result["vyne.example.ClientName"]!!.value).to.equal("Jimmy's Choos")
   }

   @Test
   fun shouldBeAbleToQueryWithShortNames() {
      val stubService = StubService()
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubService)
      val vyne = TestSchema.vyne(queryEngineFactory)

      val json = """
{
   "clientId" : "123",
   "name" : "Jimmy's Choos",
   "isicCode" : "retailer"
}"""
      val client = vyne.parseJsonModel("Client", json)
      stubService.addResponse("mockClient", client)
      vyne.addKeyValuePair("vyne.example.TaxFileNumber", "123")
      val result: QueryResult = vyne.query().find("ClientName")
      expect(result.results.size).to.equal(1)
      expect(result["vyne.example.ClientName"]!!.value).to.equal("Jimmy's Choos")
   }

   @Test
   fun shouldRetrievePropertyFromService_withMultipleAttributes_whenAttributesArePresentAsKeyValuePairs() {
      val stubService = StubService()
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubService)
      val vyne = TestSchema.vyne(queryEngineFactory)
      stubService.addResponse("creditRisk", TypedValue(vyne.getType("vyne.example.CreditRisk"), 100))
      vyne.addKeyValuePair("vyne.example.ClientId", "123")
      vyne.addKeyValuePair("vyne.example.InvoiceValue", 1000)
      val result: QueryResult = vyne.query().find("vyne.example.CreditRisk")
      expect(result.results.size).to.equal(1)
      expect(result["vyne.example.CreditRisk"]!!.value).to.equal(100)
      val paramsPassedToService: List<TypedInstance> = stubService.invocations["creditRisk"]!!
      expect(paramsPassedToService).size(2)
      expect(paramsPassedToService[0].value).to.equal("123")
      expect(paramsPassedToService[1].value).to.equal(1000)
   }


   @Test
   fun shouldRetrievePropertyFromService_withMultipleAttributes_whenAttributesAreDiscoverableViaGraph() {
      // Setup
      val stubService = StubService()
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubService)
      val vyne = TestSchema.vyne(queryEngineFactory)

      // Given...
      val json = """
{
   "clientId" : "123",
   "name" : "Jimmy's Choos",
   "isicCode" : "retailer"
}"""
      stubService.addResponse("creditRisk", TypedValue(vyne.getType("vyne.example.CreditRisk"), 100))

      val client = vyne.parseJsonModel("vyne.example.Client", json)
      stubService.addResponse("mockClient", client)

      // We know the TaxFileNumber, which we should be able to use to discover their ClientId.
      vyne.addKeyValuePair("vyne.example.TaxFileNumber", "123")
      vyne.addKeyValuePair("vyne.example.InvoiceValue", 1000)

      //When....
      val result: QueryResult = vyne.query().find("vyne.example.CreditRisk")

      // Then....
      expect(result.results.size).to.equal(1)
      expect(result["vyne.example.CreditRisk"]!!.value).to.equal(100)
      val paramsPassedToService: List<TypedInstance> = stubService.invocations["creditRisk"]!!
      expect(paramsPassedToService).size(2)
      expect(paramsPassedToService[0].value).to.equal("123")
      expect(paramsPassedToService[1].value).to.equal(1000)
   }


   @Test
   @Ignore
   fun given_notAllParamsOfOperationAreDiscoverable_then_methodNotInvoked() {
      TODO("Not sure what we should do here.")
   }

   @Test
   @Ignore
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
      TODO()
//      vyne.addData(JsonModel(client, typeName = "vyne.example.Client"))
//      val result = vyne.from(JsonModel(invoice, typeName = "vyne.example.Invoice")).find("vyne.example.ClientName")
//      expect(result.result).to.equal("Jimmy's Choos")
   }

   @Test
   fun typeInheritsCanBeUsedInSingleDirectionToAssignValues() {
      val schema = """
          type Money {
            amount:String
          }
          type TradeValue inherits Money {}

          type alias HoldReceipt as String
          service LedgerService {
            operation holdFunds(Money):HoldReceipt
          }
      """.trimIndent()

      // In the above, I should be able to get a HoldReceipt by calling
      // holdFunds() using the TradeValue, which is a type of Money

      val (vyne, stubService) = testVyne(schema)
      val tradeValue = vyne.typedValue("TradeValue", "$2.00")
      stubService.addResponse("holdFunds", vyne.typedValue("HoldReceipt", "held-123"))
      val result = vyne.query(additionalFacts = setOf(tradeValue)).find("HoldReceipt")

      expect(result.isFullyResolved).to.be.`true`
      expect(result["HoldReceipt"]!!.value).to.equal("held-123")
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

      val result = vyne.query().find("EmailAddress")

      expect(result.isFullyResolved).to.be.`true`
      expect(result["EmailAddress"]!!.value).to.equal("foo@foo.com")
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

      val result = vyne.query().find("EmailAddress[]")

      expect(result.isFullyResolved).to.be.`true`
      expect(result["EmailAddress[]".fqn().parameterizedName]!!.value).to.equal(listOf("foo@foo.com", "bar@foo.com"))
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

      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse("findPetById", vyne.typedValue("Pet", mapOf("id" to 100)))
      vyne.addKeyValuePair("lang.taxi.Int", 100)
      val result = vyne.query().find("Pet")

      expect(result.isFullyResolved).to.be.`true`
      val params = stubService.invocations["findPetById"]!!.get(0)
      expect(params.value).to.equal(100)
   }
}

fun Vyne.typedValue(typeName: String, value: Any): TypedValue {
   return TypedValue(this.getType(typeName), value)
}



