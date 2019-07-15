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
      val result = vyne.from (invoiceInstance).find("vyne.example.ClientName")
      expect(result["vyne.example.ClientName"]!!.value).to.equal("Jimmy's Choos")
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
      val instance = TypedInstance.from(vyne.schema.type("LegacyTradeNotification"), xml, vyne.schema)
      vyne.addModel(instance)
      val queryResult = vyne.query().find("NearLegNotional")
      TODO()
   }
}

fun Vyne.typedValue(typeName: String, value: Any): TypedValue {
   return TypedValue(this.getType(typeName), value)
}



