package io.vyne.tradeCompliance.aggregator

import io.vyne.demos.tradeCompliance.services.TradeValueRequest
import io.vyne.tradeCompliance.Client
import io.vyne.tradeCompliance.TradeRequest
import io.vyne.tradeCompliance.TradeValue
import io.vyne.tradeCompliance.Trader
import io.vyne.tradeCompliance.rules.*
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate

@RestController
class LegacyComplianceEvaluator(
   val restTemplate: RestTemplate = RestTemplate()
) {

   @PostMapping("/tradeCompliance/legacy")
   fun evaluate(tradeRequest: TradeRequest): TradeComplianceResult {
      val notionalRuleResponse = evaluateNotionalRule(tradeRequest)
      val jurisdictionRuleResponse = evaluateJurisdictionRule(tradeRequest)
      val tradeLimitRuleResponse = evaluateTradeLimitRule(tradeRequest)

      return TradeComplianceResult(listOf(
         notionalRuleResponse,
         jurisdictionRuleResponse,
         tradeLimitRuleResponse
      ))
   }

   private fun evaluateNotionalRule(tradeRequest: TradeRequest): NotionalLimitRuleResponse {
      return restTemplate.postForObject("http://rules-provider/rules/notionalLimits",
         NotionalLimitRuleRequest(
            tradeRequest.notional
         ),
         NotionalLimitRuleResponse::class.java
      )
   }

   private fun evaluateJurisdictionRule(tradeRequest: TradeRequest): JurisdictionRuleResponse {
      // Need to get the traders jurisdiction
      val client = restTemplate.getForObject("http://services/clients/${tradeRequest.clientId}", Client::class.java)
      // Get the clients jurisdiction
      val trader = restTemplate.getForObject("http://services/traders/${tradeRequest.traderId}", Trader::class.java)

      return restTemplate.postForObject(
         "http://rules-provider/rules/jurisdiction",
         JurisdictionRuleRequest(
            client.jurisdiction,
            trader.jurisdiction
         ),
         JurisdictionRuleResponse::class.java
      )
   }

   private fun evaluateTradeLimitRule(tradeRequest: TradeRequest): TradeValueRuleResponse {
      // Need to get the traders limit
      val trader = restTemplate.getForObject("http://services/traders/${tradeRequest.traderId}", Trader::class.java)

      // Need to get the trade value.
      val tradeValue = restTemplate.postForObject("http://services/tradeValue",
         TradeValueRequest(
            tradeRequest.notional,
            tradeRequest.price
         ), TradeValue::class.java)

      // Need to convert to USD
      val tradeValueInUsd = if (tradeRequest.price.currency != "USD") {
         restTemplate.postForObject(
            "http://services/tradeValues/USD",
            tradeValue,
            TradeValue::class.java
         )
      } else tradeValue

      return restTemplate.postForObject(
         "http://rules-provider/rules/traderLimits",
         TradeValueRuleRequest(
            tradeValueInUsd,
            trader.maxValue
         ),
         TradeValueRuleResponse::class.java)
   }


}
