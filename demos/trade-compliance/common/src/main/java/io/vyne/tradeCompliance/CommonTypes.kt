package io.vyne.tradeCompliance

import lang.taxi.annotations.DataType
import lang.taxi.annotations.ParameterType
import java.math.BigDecimal

@DataType
interface RuleEvaluationResult {
   val ruleId: String
   val status: RuleEvaluationStatus
   val message: String
}


enum class RagStatus {
   RED,
   AMBER,
   GREEN;
}

@DataType("io.vyne.RuleEvaluationStatus")
typealias RuleEvaluationStatus = RagStatus

@DataType("io.vyne.Money")
@ParameterType
data class Money(
   val currency: Currency,
   val value: MoneyAmount)

@DataType
typealias TradeValue = Money

@DataType
typealias Currency = String

@DataType
typealias MoneyAmount = BigDecimal

@DataType
typealias TraderMaxTradeValue = Money

// TODO: Make this an enum once they're better supported
@DataType
typealias CountryCode = String

@DataType("io.vyne.TraderJurisdiction")
typealias TraderJurisdiction = CountryCode

@DataType("io.vyne.ClientJurisdiction")
typealias ClientJurisdiction = CountryCode

@DataType("io.vyne.Username")
typealias Username = String

data class Trader(
   val username: Username,
   val jurisdiction: TraderJurisdiction,
   val maxValue: TraderMaxTradeValue

)

@DataType("io.vyne.ClientId")
typealias ClientId = String

data class Client(
   val id: ClientId,
   val name: String,
   val jurisdiction: ClientJurisdiction
)

@DataType("io.vyne.TradeNotional")
typealias TradeNotional = BigDecimal

@DataType("io.vyne.Price")
typealias Price = Money

@DataType
data class TradeRequest(
   val notional: TradeNotional,
   val clientId: ClientId,
   val traderId: Username,
   val price: Price
) {

}

