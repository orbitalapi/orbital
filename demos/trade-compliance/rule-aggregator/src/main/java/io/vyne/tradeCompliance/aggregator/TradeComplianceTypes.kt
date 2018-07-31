package io.vyne.tradeCompliance.aggregator

import io.vyne.tradeCompliance.RagStatus
import io.vyne.tradeCompliance.RuleEvaluationResult
import lang.taxi.annotations.DataType


@DataType("io.vyne.tradeCompliance.aggregator.RuleEvaluationResults")
typealias RuleEvaluationResults = List<RuleEvaluationResult>

@DataType
data class TradeComplianceResult(
   val results: RuleEvaluationResults
) {
   val status: TradeComplianceStatus = when {
      results.any { it.status == RagStatus.RED } -> RagStatus.RED
      results.any { it.status == RagStatus.AMBER } -> RagStatus.AMBER
      else -> RagStatus.GREEN
   }

}

@DataType("io.vyne.tradeCompliance.TradeComplianceStatus")
typealias TradeComplianceStatus = RagStatus
