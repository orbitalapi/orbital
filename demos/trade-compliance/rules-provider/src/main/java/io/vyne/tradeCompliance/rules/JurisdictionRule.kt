package io.vyne.tradeCompliance.rules

import io.vyne.tradeCompliance.ClientJurisdiction
import io.vyne.tradeCompliance.RuleEvaluationResult
import io.vyne.tradeCompliance.RuleEvaluationStatus
import io.vyne.tradeCompliance.TraderJurisdiction
import lang.taxi.annotations.DataType
import lang.taxi.annotations.Operation
import lang.taxi.annotations.Service
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@DataType
data class JurisdictionRuleRequest(
   val clientJurisdiction: ClientJurisdiction,
   val traderJurisdiction: TraderJurisdiction
)

@Service
@RestController
class JurisdictionRuleService {

   @PostMapping("/rules/jurisdiction")
   @Operation
   fun evaluate(request: JurisdictionRuleRequest): RuleEvaluationResult = evaluate(
      request.clientJurisdiction, request.traderJurisdiction
   )

   fun evaluate(clientJurisdiction: ClientJurisdiction, traderJurisdiction: TraderJurisdiction): RuleEvaluationResult {
      return if (clientJurisdiction == traderJurisdiction) {
         RuleEvaluationResult("JurisdictionRule", RuleEvaluationStatus.GREEN)
      } else {
         RuleEvaluationResult("JurisdictionRule", RuleEvaluationStatus.RED, "The trader and client must both be in the same jurisdiction")
      }
   }
}
