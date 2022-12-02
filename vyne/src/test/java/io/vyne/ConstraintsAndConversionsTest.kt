package io.vyne

import com.winterbe.expekt.expect
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.models.TypedValue
import io.vyne.models.json.parseJsonModel
import io.vyne.models.json.parseKeyValuePair
import io.vyne.query.QueryEngineFactory
import io.vyne.query.StatefulQueryEngine
import io.vyne.schemas.taxi.TaxiSchema
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.UUID
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalCoroutinesApi
class ConstraintsAndConversionsTest {
   val taxiDef = """
type Money {
   amount : Amount as Decimal
   currency : Currency as String
}

type alias Risk as Decimal
// For demonstrating constraints on request objects
type alias ClientRisk as Decimal
type alias ClientId as String

// For demonstrating constraints on request objects
parameter type ClientRiskRequest {
   amount : Money(Currency == 'GBP')
   clientId : ClientId
}

service MyService {
   @StubResponse("calculateRisk")
   operation calculateRisk(Money(this.currency == 'GBP')):Risk

   @StubResponse("convertCurrency")
   operation convertCurrency(source : Money , target : Currency) : Money( from source, this.currency == target )

   @StubResponse("calculateRiskForClient")
   operation calculateRiskForClient(ClientRiskRequest):ClientRisk
}"""

   val schema = TaxiSchema.from(taxiDef)
   // Setup

   @Test
   fun given_serviceDeclaresConstraint_then_conversionsArePerformedToSatisfyConstraintTest() {
      // Setup
      val stubService = StubService()
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(VyneCacheConfiguration.default(), emptyList(), stubService)
      val vyne = Vyne(queryEngineFactory /*,"remote:localhost/test" */).addSchema(schema)
      stubService.addResponse("convertCurrency", money(2, "GBP", vyne))
      stubService.addResponse("calculateRiskForClient", vyne.parseKeyValuePair("ClientRisk", 0.5))

      val operation = schema.service("MyService").operation("convertCurrency")
      operation.contract
      val queryEngine = vyne.queryEngine()
      queryEngine.addModel(money(5, "USD", vyne))
      queryEngine.addModel(vyne.parseKeyValuePair("ClientId","1234"))
      val result = runBlocking { queryEngine.queryContext(queryId = UUID.randomUUID().toString(), clientQueryId = null).find("ClientRisk").results.toList() }

      expect(stubService.invocations).to.contain.keys("convertCurrency")
      expect(stubService.invocations).to.contain.keys("calculateRiskForClient")
      val calculateRiskCallParam: TypedObject = stubService.invocations["calculateRiskForClient"]!!.first() as TypedObject
      expect(calculateRiskCallParam.type.name.name).to.equal("ClientRiskRequest")

      val moneyParam = calculateRiskCallParam["amount"] as TypedObject
      expect(moneyParam["amount"].value).to.equal(2.toBigDecimal())
      expect(moneyParam["currency"].value).to.equal("GBP")
      return
   }

   @Test
   fun given_serviceDeclaresRequestObjectWithConstraints_then_conversionsArePerformedToSatisfyConstraint() {
      val stubService = StubService()
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(VyneCacheConfiguration.default(), emptyList(), stubService)
      val vyne = Vyne(queryEngineFactory).addSchema(schema)
      stubService.addResponse("convertCurrency", money(2, "GBP", vyne))
      stubService.addResponse("calculateRisk", vyne.parseKeyValuePair("Risk", 0.5))

      val queryEngine = vyne.queryEngine()
      queryEngine.addModel(money(5, "USD", vyne))
      val result = runBlocking { queryEngine.queryContext(queryId = UUID.randomUUID().toString(), clientQueryId = null).find("Risk").results.toList() }

      expect(stubService.invocations).to.contain.keys("convertCurrency")
      expect(stubService.invocations).to.contain.keys("calculateRisk")
      val calculateRiskCallParam: TypedObject = stubService.invocations["calculateRisk"]!!.first() as TypedObject
      expect(calculateRiskCallParam["amount"]!!.value).to.equal(2.toBigDecimal())
      expect(calculateRiskCallParam["currency"]!!.value).to.equal("GBP")

   }

   private fun money(amount: Int, currency: String, vyne: Vyne): TypedInstance {
      return vyne.parseJsonModel("Money", """{ "amount" : $amount, "currency" : "$currency" }""")
   }

   fun suspendTest(body: suspend CoroutineScope.() -> Unit) {
      runBlocking { body() }
   }


   @Test
   fun given_paramIsOfWrongType_and_typeConversionServiceExists_that_itIsConverted()  {
      val taxiDef = """
type alias UkSic2003 as String
type alias UkSic2007 as String
type alias Foo as String
service TestService {
   @StubResponse("calculateFoo")
   operation calculateFoo(UkSic2007):Foo
   @StubResponse("convertUkSic")
   operation convertUkSic(UkSic2003):UkSic2007
}
"""
      // Setup
      val stubService = StubService()
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(VyneCacheConfiguration.default(), emptyList(), stubService)
      val vyne = Vyne(queryEngineFactory).addSchema(TaxiSchema.from(taxiDef))
      stubService.addResponse("calculateFoo", vyne.parseKeyValuePair("Foo", "Hello"))
      stubService.addResponse("convertUkSic", vyne.parseKeyValuePair("UkSic2007", "2007-Fully-Sick"))

      val queryEngine = vyne.queryEngine()
      queryEngine.addModel(vyne.parseKeyValuePair("UkSic2003","SickOf2003"))
      val resultList = runBlocking { queryEngine.queryContext(queryId = UUID.randomUUID().toString(), clientQueryId = null).find("Foo").results.toList() }

      expect( (resultList.get(0) as TypedValue).value).to.equal("Hello")
      // Assert correct params were passed
      expect(stubService.invocations["convertUkSic"]!!.first().value).to.equal("SickOf2003")
      expect(stubService.invocations["calculateFoo"]!!.first().value).to.equal("2007-Fully-Sick")
   }
   @Test
   fun given_requestObjectContainsParamOfWrongType_and_typeConversionServiceExists_that_itIsConverted() {
      val taxiDef = """
type alias UkSic2003 as String
type alias UkSic2007 as String
type alias Foo as String
parameter type RequestObject {
   input : UkSic2007
}
service TestService {
   @StubResponse("calculateFoo")
   operation calculateFoo(RequestObject):Foo
   @StubResponse("convertUkSic")
   operation convertUkSic(UkSic2003):UkSic2007
}
"""
      // Setup
      val stubService = StubService()
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(VyneCacheConfiguration.default(), emptyList(), stubService)
      val vyne = Vyne(queryEngineFactory).addSchema(TaxiSchema.from(taxiDef))
      stubService.addResponse("calculateFoo", vyne.parseKeyValuePair("Foo", "Hello"))
      stubService.addResponse("convertUkSic", vyne.parseKeyValuePair("UkSic2007", "2007-Fully-Sick"))

      val queryEngine: StatefulQueryEngine = vyne.queryEngine()
      queryEngine.addModel(vyne.parseKeyValuePair("UkSic2003","SickOf2003"))
      val result = runBlocking{ queryEngine.queryContext(queryId = UUID.randomUUID().toString(), clientQueryId = null).find("Foo").results.toList() }

      //expect(result["Foo"]!!.value).to.equal("Hello")
      // Assert correct params were passed
      expect(stubService.invocations["convertUkSic"]!!.first().value).to.equal("SickOf2003")
      val requestObject = stubService.invocations["calculateFoo"]!!.first() as TypedObject
      expect(requestObject.type.name.fullyQualifiedName).to.equal("RequestObject")
      expect(requestObject["input"].value).to.equal("2007-Fully-Sick")
   }


}

