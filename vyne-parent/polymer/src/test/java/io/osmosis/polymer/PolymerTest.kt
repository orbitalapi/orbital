package io.osmosis.polymer

import com.winterbe.expekt.expect
import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.models.TypedValue
import io.osmosis.polymer.models.json.addJsonModel
import io.osmosis.polymer.models.json.addKeyValuePair
import io.osmosis.polymer.models.json.parseJsonModel
import io.osmosis.polymer.query.*
import io.osmosis.polymer.schemas.taxi.TaxiSchema
import org.junit.Test

object TestSchema {
   val taxiDef = """
namespace polymer.example
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


   fun polymer(queryEngineFactory: QueryEngineFactory = QueryEngineFactory.default()) = Polymer(queryEngineFactory).addSchema(schema)
   val queryParser = QueryParser(schema)

   fun typeNode(name: String): Set<QuerySpecTypeNode> {
      return queryParser.parse(name)
   }

   fun queryContext(): QueryContext = polymer().query().queryContext()
}

fun testPolymer(schema: TaxiSchema): Pair<Polymer, StubService> {
   val stubService = StubService()
   val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubService)
   val polymer = Polymer(queryEngineFactory).addSchema(schema)
   return polymer to stubService
}

fun testPolymer(schema: String) = testPolymer(TaxiSchema.from(schema))

class PolymerTest {


   @Test
   fun shouldFindAPropertyOnAnObject() {

      val polymer = TestSchema.polymer()
      val json = """
{
   "clientId" : "123",
   "name" : "Jimmy's Choos",
   "isicCode" : "retailer"
}"""
      polymer.addJsonModel("polymer.example.Client", json)
      val result = polymer.query().find("polymer.example.ClientName")
      expect(result.results.size).to.equal(1)
      expect(result["polymer.example.ClientName"]!!.value).to.equal("Jimmy's Choos")
   }

   @Test
   fun shouldRetrievePropertyFromService() {
      val stubService = StubService()
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubService)
      val polymer = TestSchema.polymer(queryEngineFactory)

      val json = """
{
   "clientId" : "123",
   "name" : "Jimmy's Choos",
   "isicCode" : "retailer"
}"""
      val client = polymer.parseJsonModel("polymer.example.Client", json)
      stubService.addResponse("mockClient", client)
      polymer.addKeyValuePair("polymer.example.TaxFileNumber", "123")
      val result: QueryResult = polymer.query().find("polymer.example.ClientName")
      expect(result.results.size).to.equal(1)
      expect(result["polymer.example.ClientName"]!!.value).to.equal("Jimmy's Choos")
   }

   @Test
   fun shouldRetrievePropertyFromService_withMultipleAttributes_whenAttributesArePresentAsKeyValuePairs() {
      val stubService = StubService()
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubService)
      val polymer = TestSchema.polymer(queryEngineFactory)
      stubService.addResponse("creditRisk", TypedValue(polymer.getType("polymer.example.CreditRisk"), 100))
      polymer.addKeyValuePair("polymer.example.ClientId", "123")
      polymer.addKeyValuePair("polymer.example.InvoiceValue", 1000)
      val result: QueryResult = polymer.query().find("polymer.example.CreditRisk")
      expect(result.results.size).to.equal(1)
      expect(result["polymer.example.CreditRisk"]!!.value).to.equal(100)
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
      val polymer = TestSchema.polymer(queryEngineFactory)

      // Given...
      val json = """
{
   "clientId" : "123",
   "name" : "Jimmy's Choos",
   "isicCode" : "retailer"
}"""
      stubService.addResponse("creditRisk", TypedValue(polymer.getType("polymer.example.CreditRisk"), 100))

      val client = polymer.parseJsonModel("polymer.example.Client", json)
      stubService.addResponse("mockClient", client)

      // We know the TaxFileNumber, which we should be able to use to discover their ClientId.
      polymer.addKeyValuePair("polymer.example.TaxFileNumber", "123")
      polymer.addKeyValuePair("polymer.example.InvoiceValue", 1000)

      //When....
      val result: QueryResult = polymer.query().find("polymer.example.CreditRisk")

      // Then....
      expect(result.results.size).to.equal(1)
      expect(result["polymer.example.CreditRisk"]!!.value).to.equal(100)
      val paramsPassedToService: List<TypedInstance> = stubService.invocations["creditRisk"]!!
      expect(paramsPassedToService).size(2)
      expect(paramsPassedToService[0].value).to.equal("123")
      expect(paramsPassedToService[1].value).to.equal(1000)
   }


   @Test
   fun given_notAllParamsOfOperationAreDiscoverable_then_methodNotInvoked() {
      TODO("Not sure what we should do here.")
   }

   @Test
   fun shouldFindAPropertyValueByWalkingADirectRelationship() {

      val polymer = TestSchema.polymer()
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
//      polymer.addData(JsonModel(client, typeName = "polymer.example.Client"))
//      val result = polymer.from(JsonModel(invoice, typeName = "polymer.example.Invoice")).find("polymer.example.ClientName")
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

      val (polymer, stubService) = testPolymer(schema)
      val tradeValue = polymer.typedValue("TradeValue", "$2.00")
      stubService.addResponse("holdFunds", polymer.typedValue("HoldReceipt", "held-123"))
      val result = polymer.query().find("HoldReceipt", setOf(tradeValue))

      expect(result.isFullyResolved).to.be.`true`
      expect(result["HoldReceipt"]!!.value).to.equal("held-123")
   }
}

fun Polymer.typedValue(typeName: String, value: Any): TypedValue {
   return TypedValue(this.getType(typeName), value)
}



