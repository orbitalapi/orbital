package io.vyne.tradeCompliance.rules

import io.vyne.tradeCompliance.RuleEvaluationResult
import io.vyne.tradeCompliance.RuleEvaluationStatus
import io.vyne.tradeCompliance.TradeValue
import io.vyne.tradeCompliance.TraderMaxTradeValue
import lang.taxi.annotations.*
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@DataType
@ParameterType
data class TradeValueRuleRequest(
   @Constraint("currency = 'USD'")
   val tradeValue: TradeValue,
   val traderLimit: TraderMaxTradeValue
)

@Service
@RestController
class TradeValueRuleService {

   @Operation
   @PostMapping
   fun evaluate(@RequestBody request: TradeValueRuleRequest) = evaluate(request.tradeValue, request.traderLimit)

   // Specify a rule that the tradeValue must be in the same currency as the traderLimit
   fun evaluate(tradeValue: TradeValue, traderLimit: TraderMaxTradeValue): TradeValueRuleResponse {
      require(tradeValue.currency == traderLimit.currency) { "The trade value and trade limit must be in the same currency" }
      return if (tradeValue.value < traderLimit.value) {
         TradeValueRuleResponse("TradeValueRule", RuleEvaluationStatus.GREEN, "OK")
      } else {
         TradeValueRuleResponse("TradeValueRule", RuleEvaluationStatus.RED, "Trade value exceeds trader limit")
      }
   }
}


@DataType
data class TradeValueRuleResponse(
   override val ruleId: String,
   override val status: RuleEvaluationStatus,
   override val message: String
) : RuleEvaluationResult
