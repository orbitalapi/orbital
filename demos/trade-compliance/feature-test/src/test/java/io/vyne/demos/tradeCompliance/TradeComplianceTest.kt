package io.vyne.demos.tradeCompliance

import com.winterbe.expekt.expect
import io.osmosis.polymer.Polymer
import io.osmosis.polymer.StubService
import io.osmosis.polymer.models.TypedCollection
import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.models.TypedObject
import io.osmosis.polymer.models.json.parseJsonModel
import io.osmosis.polymer.query.QueryEngineFactory
import io.osmosis.polymer.schemas.taxi.TaxiSchema
import org.apache.commons.io.IOUtils
import org.junit.Before
import org.junit.Test

class TradeComplianceTest {

   lateinit var stubService: StubService
   lateinit var polymer: Polymer

   lateinit var tradeRequest: TypedInstance

   val client = """
       |{
       |"id" : "GBP_Client",
       |    "name" : "Jimmy Client",
       |    "jurisdiction" : "GBP"
       |}
   """.trimMargin()

   fun ruleResult(ruleId: String) = """
       {
         "ruleId" : "$ruleId",
         "status" : "GREEN",
         "message" : "All looks good"
       }
   """.trimIndent()

   val trader = """
       {
         "username" : "EUR_Trader",
         "jurisdiction" : "EUR",
         "limit" : {
            "currency" : "USD",
            "value" : 100
         }
       }
   """.trimIndent()

   @Before
   fun setup() {
      val schemaDef = this.javaClass.getResourceAsStream("/schema.taxi")
      val schema = TaxiSchema.from(IOUtils.toString(schemaDef))
      stubService = StubService()

      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubService)

      polymer = Polymer(queryEngineFactory).addSchema(schema)

      val tradeJson = """{
         "notional" :   1000,
          "clientId" : "GBP_Client",
          "traderId" : "EUR_Trader",
          "price" : {
               "currency" : "GBP",
               "value" : 0.55
          },
          "tradeValue" : {
                "currency" : "GBP",
               "value" : 55.00
          }

      }""".trimIndent()

      val tradeValueInUsd = """{
                "currency" : "USD",
               "value" : 100.00
          }"""


      tradeRequest = polymer.parseJsonModel("io.vyne.TradeRequest", tradeJson)


      stubService.addResponse("getClient", polymer.parseJsonModel("io.vyne.Client", client))
      stubService.addResponse("jurisdictionRuleService", polymer.parseJsonModel("io.vyne.JurisdictionRuleResult", ruleResult("jurisdictionRuleResult")))
      stubService.addResponse("tradeValueRuleService", polymer.parseJsonModel("io.vyne.TradeValueRuleResult", ruleResult("tradeValueRuleResult")))
      stubService.addResponse("notionalLimitRuleService", polymer.parseJsonModel("io.vyne.NotionalLimitRuleResult", ruleResult("notionalLimitRuleResult")))
      stubService.addResponse("getTrader", polymer.parseJsonModel("io.vyne.Trader", trader))
      stubService.addResponse("convertRates", polymer.parseJsonModel("io.vyne.TradeValue", tradeValueInUsd))
   }

   @Test
   fun canFindTraderMaxValue() {
      // This test is interesting because we're finding a value within a returned value,
      // rather than the returned value itself.
      // This differs from other queries, because in other queries the result from the service is
      // what's used.  Here, we get a result from a query, and then need to inspect a value within it.
      val result = polymer.query().find("io.vyne.TraderMaxTradeValue", setOf(tradeRequest))
      expect(result.isFullyResolved).to.be.`true`

   }

   @Test
   fun canEvaluateJurisdictionRule() {
      val result = polymer.query().find("io.vyne.JurisdictionRuleResult", setOf(tradeRequest))
      expect(result.isFullyResolved).to.be.`true`
      val ruleEvaluationResult = result.get("io.vyne.JurisdictionRuleResult") as TypedObject
      expect(ruleEvaluationResult["message"]!!.value).to.equal("All looks good")

      expect(stubService.invocations["getTrader"]).to.have.size(1)
      expect(stubService.invocations["getClient"]).to.have.size(1)
      expect(stubService.invocations["jurisdictionRuleService"]).to.have.size(1)
      val ruleServiceRequest = stubService.invocations["jurisdictionRuleService"]!!.first() as TypedObject
      expect(ruleServiceRequest["clientJurisdiction"]!!.value).to.equal("GBP")
      expect(ruleServiceRequest["traderJurisdiction"]!!.value).to.equal("EUR")
   }

   @Test
   fun canEvaluationTradeValueRule() {
      val result = polymer.query().find("io.vyne.TradeValueRuleResult", setOf(tradeRequest))
      expect(result.isFullyResolved).to.be.`true`
   }

   @Test
   fun canEvaluateNotionalLimitRule() {
      val result = polymer.query().find("io.vyne.NotionalLimitRuleResult", setOf(tradeRequest))
      expect(result.isFullyResolved).to.be.`true`
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
      val queryResult = polymer.query().gather(ruleEvaluationResult, setOf(tradeRequest))

      expect(queryResult.isFullyResolved).to.be.`true`
      val result = queryResult["io.vyne.RuleEvaluationResult"] as TypedCollection
      expect(result.size).to.equal(3)

      require(result.any { collectionMember -> collectionMember.type.fullyQualifiedName == "io.vyne.NotionalLimitRuleResult" } )
      require(result.any { collectionMember -> collectionMember.type.fullyQualifiedName == "io.vyne.JurisdictionRuleResult" } )
      require(result.any { collectionMember -> collectionMember.type.fullyQualifiedName == "io.vyne.TradeValueRuleResult" } )
   }
}
