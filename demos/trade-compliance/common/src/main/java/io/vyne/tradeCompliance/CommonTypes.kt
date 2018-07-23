package io.vyne.tradeCompliance

import lang.taxi.annotations.DataType
import lang.taxi.annotations.ParameterType
import java.math.BigDecimal

@DataType
data class RuleEvaluationResult(
   val ruleId: String,
   val status: RuleEvaluationStatus,
   val message: String? = null
)


enum class RagStatus {
   RED,
   AMBER,
   GREEN;
}
typealias RuleEvaluationStatus = RagStatus

@DataType("io.vyne.Money")
@ParameterType
data class Money(
   @field:DataType("io.vyne.Currency") val currency: String,
   @field:DataType("io.vyne.MoneyAmount") val value: BigDecimal)

@DataType
typealias TradeValue = Money

@DataType
typealias TraderMaxTradeValue = Money

// TODO: Make this an enum once they're better supported
@DataType
typealias CountryCode = String

@DataType("foo.traderJuris")
typealias TraderJurisdiction = CountryCode

@DataType
typealias ClientJurisdiction = CountryCode


@DataType
data class TradeRequest(
   @field:DataType("io.vyne.TradeNotional")
   val notional: BigDecimal
)

