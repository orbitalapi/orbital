package io.vyne.demos.tradeCompliance

import io.osmosis.polymer.Polymer
import io.osmosis.polymer.StubService
import io.osmosis.polymer.TestSchema.schema
import io.osmosis.polymer.models.json.parseJsonModel
import io.osmosis.polymer.query.QueryEngineFactory
import io.osmosis.polymer.schemas.Schema
import io.osmosis.polymer.schemas.taxi.TaxiSchema
import org.apache.commons.io.IOUtils
import org.junit.Before
import org.junit.Test
import kotlin.test.fail

class TradeComplianceTest {

   lateinit var stubService: StubService
   lateinit var polymer: Polymer
   @Before
   fun setup() {
      val schemaDef = this.javaClass.getResourceAsStream("/schema.taxi")
      val schema = TaxiSchema.from(IOUtils.toString(schemaDef))
      stubService = StubService()

      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubService)
      polymer = Polymer(queryEngineFactory).addSchema(schema)

   }

   @Test
   fun canDiscoverTradeComplianceStatus() {
      val tradeJson = """{
         "notional" :   1000,
          "clientId" : "GBP_Client",
          "traderId" : "EUR_Trader",
          "price" : {
               "currency" : "GBP",
               "value" : 0.55
          }

      }""".trimIndent()

      val tradeRequest = polymer.parseJsonModel("io.vyne.tradeCompliance.aggregator.TradeRequest", tradeJson)
//      val result = polymer.query().find("io.vyne.tradeCompliance.aggregator.TradeComplianceResult", setOf(tradeRequest))
      val ruleEvaluationResult = "io.vyne.tradeCompliance.rules.RuleEvaluationResult"
      val ruleEvaluationResults = "io.vyne.tradeCompliance.aggregator.RuleEvaluationResults"
      val result = polymer.query().find(ruleEvaluationResult, setOf(tradeRequest))

      fail()
   }
}
