package io.vyne.tradeCompliance.rules

import io.vyne.tradeCompliance.ClientJurisdiction
import io.vyne.tradeCompliance.RuleEvaluationResult
import io.vyne.tradeCompliance.RuleEvaluationStatus
import io.vyne.tradeCompliance.TraderJurisdiction
import lang.taxi.annotations.DataType
import lang.taxi.annotations.Operation
import lang.taxi.annotations.ParameterType
import lang.taxi.annotations.Service
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@DataType
@ParameterType
data class JurisdictionRuleRequest(
   val clientJurisdiction: ClientJurisdiction,
   val traderJurisdiction: TraderJurisdiction
)

@Service
@RestController
class JurisdictionRuleService {

   @PostMapping("/rules/jurisdiction")
   @Operation
   fun evaluate(@RequestBody request: JurisdictionRuleRequest) = evaluate(
      request.clientJurisdiction, request.traderJurisdiction
   )

   fun evaluate(clientJurisdiction: ClientJurisdiction, traderJurisdiction: TraderJurisdiction): JurisdictionRuleResponse {
      return if (clientJurisdiction == traderJurisdiction) {
         JurisdictionRuleResponse("JurisdictionRule", RuleEvaluationStatus.GREEN, "OK")
      } else {
         JurisdictionRuleResponse("JurisdictionRule", RuleEvaluationStatus.RED, "The trader and client must both be in the same jurisdiction")
      }
   }
}

@DataType
data class JurisdictionRuleResponse(
   override val ruleId: String,
   override val status: RuleEvaluationStatus,
   override val message: String
) : RuleEvaluationResult
