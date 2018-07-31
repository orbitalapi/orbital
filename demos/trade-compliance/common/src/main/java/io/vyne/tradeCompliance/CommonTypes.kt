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

@DataType("io.vyne.RuleEvaluationStatus")
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

@DataType("io.vyne.TraderJurisdiction")
typealias TraderJurisdiction = CountryCode

@DataType("io.vyne.ClientJurisdiction")
typealias ClientJurisdiction = CountryCode

@DataType("io.vyne.Username")
typealias Username = String

data class User(
   val username: Username,
   val jurisdiction: CountryCode
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
)

