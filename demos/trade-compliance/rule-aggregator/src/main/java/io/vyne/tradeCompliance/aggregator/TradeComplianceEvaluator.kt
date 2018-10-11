package io.vyne.tradeCompliance.aggregator

import io.vyne.VyneClient
import io.vyne.tradeCompliance.RuleEvaluationResult
import io.vyne.tradeCompliance.TradeRequest
import lang.taxi.annotations.Operation
import lang.taxi.annotations.Service
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Service
class TradeComplianceEvaluator(val vyne: VyneClient) {

   @PostMapping("/tradeCompliance")
   @Operation
   fun evaluate(@RequestBody tradeRequest: TradeRequest): TradeComplianceResult {
      val ruleEvaluations = vyne
         .given(tradeRequest)
         .gather<RuleEvaluationResult>()

      return TradeComplianceResult(ruleEvaluations)
   }
}
