package io.osmosis.polymer

import com.winterbe.expekt.expect
import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.models.TypedObject
import io.osmosis.polymer.models.json.parseJsonModel
import io.osmosis.polymer.models.json.parseKeyValuePair
import io.osmosis.polymer.query.QueryEngineFactory
import io.osmosis.polymer.schemas.taxi.TaxiSchema
import org.junit.Test

class QueryingWithConstraintsTest {
   val taxiDef = """
type Money {
   amount : Amount as Decimal
   currency : Currency as String
}

type alias Risk as Decimal

service MyService {
   @StubResponse("calculateRisk")
   operation calculateRisk(Money(currency = 'GBP')):Risk

   @StubResponse("convertCurrency")
   operation convertCurrency(source : Money , target : Currency) : Money( from source, currency = target )

}"""

   val schema = TaxiSchema.from(taxiDef)
   // Setup

   @Test
   fun given_serviceDeclaresConstraint_that_conversionsArePerformedToSatisfyConstraint() {
      // Setup
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


   private fun Polymer.money(amount: Int, currency: String) {

   }

}
