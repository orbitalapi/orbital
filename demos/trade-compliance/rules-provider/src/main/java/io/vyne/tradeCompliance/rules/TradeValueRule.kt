package io.vyne.tradeCompliance.rules

import io.vyne.tradeCompliance.RuleEvaluationResult
import io.vyne.tradeCompliance.RuleEvaluationStatus
import io.vyne.tradeCompliance.TradeValue
import io.vyne.tradeCompliance.TraderMaxTradeValue
import lang.taxi.annotations.DataType
import lang.taxi.annotations.Operation
import lang.taxi.annotations.Service
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@DataType
data class TradeValueRuleRequest(
   val tradeValue: TradeValue,
   val traderLimit: TraderMaxTradeValue
)

@Service
@RestController
class TradeValueRuleService {

   @Operation
   @PostMapping
   fun evaluate(request: TradeValueRuleRequest) = evaluate(request.tradeValue, request.traderLimit)

   // Specify a rule that the tradeValue must be in the same currency as the traderLimit
   fun evaluate(tradeValue: TradeValue, traderLimit: TraderMaxTradeValue): RuleEvaluationResult {
      require(tradeValue.currency == traderLimit.currency) { "The trade value and trade limit must be in the same currency" }
      return if (tradeValue.value < traderLimit.value) {
         RuleEvaluationResult("TradeValueRule", RuleEvaluationStatus.GREEN)
      } else {
         RuleEvaluationResult("TradeValueRule", RuleEvaluationStatus.RED, "Trade value exceeds trader limit")
      }
   }
}
