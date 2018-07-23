package io.vyne.tradeCompliance.rules

import io.vyne.tradeCompliance.RuleEvaluationResult
import io.vyne.tradeCompliance.RuleEvaluationStatus
import lang.taxi.annotations.DataType
import lang.taxi.annotations.Operation
import lang.taxi.annotations.Service
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@DataType
data class NotionalLimitRuleRequest(
   val notional: BigDecimal
)

@RestController
@Service
class NotionalLimitRuleService {
   companion object {
      val NOTIONAL_LIMIT = BigDecimal.valueOf(1_000_000)
      val RULE_ID = "NotionalLimitRule"
   }

   @PostMapping("/rules/notionalLimits")
   @Operation
   fun evaluate(request: NotionalLimitRuleRequest) = evaluate(request.notional)

   fun evaluate(notional: BigDecimal): RuleEvaluationResult {
      return if (notional < NOTIONAL_LIMIT) {
         RuleEvaluationResult(RULE_ID, RuleEvaluationStatus.GREEN)
      } else {
         RuleEvaluationResult(RULE_ID, RuleEvaluationStatus.RED, "Notional limit exceeded")
      }
   }
}

