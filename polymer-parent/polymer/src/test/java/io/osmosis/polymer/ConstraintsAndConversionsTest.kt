package io.osmosis.polymer

import com.winterbe.expekt.expect
import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.models.TypedObject
import io.osmosis.polymer.models.json.parseJsonModel
import io.osmosis.polymer.models.json.parseKeyValuePair
import io.osmosis.polymer.query.QueryEngineFactory
import io.osmosis.polymer.query.QueryResult
import io.osmosis.polymer.schemas.taxi.TaxiSchema
import org.junit.Test

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
   amount : Money(currency = 'GBP')
   clientId : ClientId
}

service MyService {
   @StubResponse("calculateRisk")
   operation calculateRisk(Money(currency = 'GBP')):Risk

   @StubResponse("convertCurrency")
   operation convertCurrency(source : Money , target : Currency) : Money( from source, currency = target )

   @StubResponse("calculateRiskForClient")
   operation calculateRiskForClient(ClientRiskRequest):ClientRisk
}"""

   val schema = TaxiSchema.from(taxiDef)
   // Setup

   @Test
   fun given_serviceDeclaresConstraint_then_conversionsArePerformedToSatisfyConstraint() {
      // Setup
      val stubService = StubService()
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubService)
      val polymer = Polymer(queryEngineFactory /*,"remote:localhost/test" */).addSchema(schema)
      stubService.addResponse("convertCurrency", money(2, "GBP", polymer))
      stubService.addResponse("calculateRiskForClient", polymer.parseKeyValuePair("Risk", 0.5))

      val queryContext = polymer.query()
      queryContext.addModel(money(5, "USD", polymer))
      queryContext.addModel(polymer.parseKeyValuePair("ClientId","1234"))
      val result = queryContext.find("ClientRisk")

      expect(stubService.invocations).to.contain.keys("convertCurrency")
      expect(stubService.invocations).to.contain.keys("calculateRiskForClient")
      val calculateRiskCallParam: TypedObject = stubService.invocations["calculateRiskForClient"]!!.first() as TypedObject
      expect(calculateRiskCallParam["amount"].value).to.equal(2)
      expect(calculateRiskCallParam["currency"].value).to.equal("GBP")
   }

   @Test
   fun given_serviceDeclaresRequestObjectWithConstraints_then_conversionsArePerformedToSatisfyConstraint() {
      val stubService = StubService()
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubService)
      val polymer = Polymer(queryEngineFactory).addSchema(schema)
      stubService.addResponse("convertCurrency", money(2, "GBP", polymer))
      stubService.addResponse("calculateRisk", polymer.parseKeyValuePair("Risk", 0.5))

      val queryContext = polymer.query()
      queryContext.addModel(money(5, "USD", polymer))
      val result = queryContext.find("Risk")

      expect(stubService.invocations).to.contain.keys("convertCurrency")
      expect(stubService.invocations).to.contain.keys("calculateRisk")
      val calculateRiskCallParam: TypedObject = stubService.invocations["calculateRisk"]!!.first() as TypedObject
      expect(calculateRiskCallParam["amount"]!!.value).to.equal(2)
      expect(calculateRiskCallParam["currency"]!!.value).to.equal("GBP")

   }

   private fun money(amount: Int, currency: String, polymer: Polymer): TypedInstance {
      return polymer.parseJsonModel("Money", """{ "amount" : $amount, "currency" : "$currency" }""")
   }


   @Test
   fun given_paramIsOfWrongType_and_typeConversionServiceExists_that_itIsConverted() {
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
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubService)
      val polymer = Polymer(queryEngineFactory).addSchema(TaxiSchema.from(taxiDef))
      stubService.addResponse("calculateFoo", polymer.parseKeyValuePair("Foo", "Hello"))
      stubService.addResponse("convertUkSic", polymer.parseKeyValuePair("UkSic2007", "2007-Fully-Sick"))

      val queryContext = polymer.query()
      queryContext.addModel(polymer.parseKeyValuePair("UkSic2003","SickOf2003"))
      val result: QueryResult = queryContext.find("Foo")

      expect(result["Foo"]!!.value).to.equal("Hello")
      // Assert correct params were passed
      expect(stubService.invocations["convertUkSic"]!!.first().value).to.equal("SickOf2003")
      expect(stubService.invocations["calculateFoo"]!!.first().value).to.equal("2007-Fully-Sick")
   }


}
