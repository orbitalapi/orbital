package io.vyne.demos.tradeCompliance

import com.winterbe.expekt.expect
import io.osmosis.polymer.Polymer
import io.osmosis.polymer.StubService
import io.osmosis.polymer.models.TypedObject
import io.osmosis.polymer.models.json.parseJsonModel
import io.osmosis.polymer.query.EdgeNavigator
import io.osmosis.polymer.query.HipsterDiscoverGraphQueryStrategy
import io.osmosis.polymer.query.QueryEngineFactory
import io.osmosis.polymer.schemas.taxi.TaxiSchema
import org.apache.commons.io.IOUtils
import org.junit.Before
import org.junit.Test
import kotlin.test.fail

class TradeComplianceTest {

   lateinit var stubService: StubService
   lateinit var polymer: Polymer

   lateinit var graphQueryStrategy: HipsterDiscoverGraphQueryStrategy

   val client = """
       |{
       |"id" : "GBP_Client",
       |    "name" : "Jimmy Client",
       |    "jurisdiction" : "GBP"
       |}
   """.trimMargin()

   val jurisdictionRuleResult = """
       {
         "ruleId" : "jurisdictionRule",
         "status" : "GREEN",
         "message" : "All looks good"
       }
   """.trimIndent()

   val trader = """
       {
         "username" : "EUR_Trader",
         "jurisdiction" : "EUR"
       }
   """.trimIndent()

   @Before
   fun setup() {
      val schemaDef = this.javaClass.getResourceAsStream("/schema.taxi")
      val schema = TaxiSchema.from(IOUtils.toString(schemaDef))
      stubService = StubService()

      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubService)

      graphQueryStrategy = HipsterDiscoverGraphQueryStrategy(
         EdgeNavigator(QueryEngineFactory.edgeEvaluators(listOf(stubService)))
      )

      polymer = Polymer(queryEngineFactory).addSchema(schema)


      stubService.addResponse("getClient", polymer.parseJsonModel("io.vyne.Client", client))
      stubService.addResponse("jurisdictionRuleService", polymer.parseJsonModel("io.vyne.RuleEvaluationResult", jurisdictionRuleResult))
      stubService.addResponse("getTrader", polymer.parseJsonModel("io.vyne.Trader", trader))
   }

   @Test
   fun canEvaluateJurisdictionRule() {
      val tradeJson = """{
         "notional" :   1000,
          "clientId" : "GBP_Client",
          "traderId" : "EUR_Trader",
          "price" : {
               "currency" : "GBP",
               "value" : 0.55
          }

      }""".trimIndent()


      val tradeRequest = polymer.parseJsonModel("io.vyne.TradeRequest", tradeJson)

      val result = polymer.query().find("io.vyne.RuleEvaluationResult", setOf(tradeRequest))
      expect(result.isFullyResolved).to.be.`true`
      val ruleEvaluationResult = result.get("io.vyne.RuleEvaluationResult") as TypedObject
      expect(ruleEvaluationResult["message"]!!.value).to.equal("All looks good")

      expect(stubService.invocations["getTrader"]).to.have.size(1)
      expect(stubService.invocations["getClient"]).to.have.size(1)
      expect(stubService.invocations["jurisdictionRuleService"]).to.have.size(1)
      val ruleServiceRequest = stubService.invocations["jurisdictionRuleService"]!!.first() as TypedObject
      expect(ruleServiceRequest["clientJurisdiction"]!!.value).to.equal("GBP")
      expect(ruleServiceRequest["traderJurisdiction"]!!.value).to.equal("EUR")
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

      val tradeRequest = polymer.parseJsonModel("io.vyne.TradeRequest", tradeJson)
//      val result = polymer.query().find("io.vyne.tradeCompliance.aggregator.TradeComplianceResult", setOf(tradeRequest))
      val ruleEvaluationResult = "io.vyne.RuleEvaluationResult"
      val ruleEvaluationResults = "io.vyne.RuleEvaluationResults"
      val result = polymer.query().gather(ruleEvaluationResult, setOf(tradeRequest))

      fail()
   }
}
