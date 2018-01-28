package io.vyne.demos.conversion.tradeThreshold

import io.osmosis.polymer.utils.log
import lang.taxi.annotations.*
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@DataType("polymer.threshold.Notional")
data class Notional(
   @field:DataType("polymer.threshold.NotionalValue")
   val value:BigDecimal,
   @field:DataType("polymer.threshold.CurrencySymbol")
   val currency:String) {
   override fun toString(): String {
      return "$currency ${value.toPlainString()}"
   }
}

@DataType("polymer.threshold.TradeThresholdRequest")
data class TradeThresholdRequest(val notional:Notional)

@DataType("polymer.threshold.TradeThresholdResponse")
data class TradeThresholdResponse(
   val notional: Notional,
   @field:DataType("polymer.threshold.ExceedsThreshold")
   val exceedsThreshold:Boolean)

@DataType("polymer.threshold.ConversionRequest")
data class ConversionRequest(val notional: Notional,
                             @field:DataType("polymer.threshold.CurrencySymbol")
                             val targetCurrency:String)
@RestController
@Service
class TradeThresholdService {

   @PostMapping("/thresholds")
   @Operation
   fun getThresholdExceeded(@RequestBody request:TradeThresholdRequest):TradeThresholdResponse {
      if (request.notional.currency != "USD") error("Currency must be USD")
      return TradeThresholdResponse(request.notional,request.notional.value.intValueExact() >= 1_000_000)
   }
}

@RestController
@Service
class NotionalConversionService {
   @PostMapping("/conversion")
   @Operation
   @ResponseContract(
      basedOn = "request.notional",
      constraints = ResponseConstraint("currency = request.targetCurrency")
   )
   fun convertNotional(@RequestBody request: ConversionRequest):Notional {
      val symbol = request.notional.currency + "/" + request.targetCurrency
      val conversionRate = when (symbol) {
         "GBP/USD" -> 0.8
         "EUR/USD" -> 0.7
         "AUD/USD" -> 0.6
         else -> error("Conversion for symbol $symbol not supported")
      }
      val convertedValue = request.notional.value.multiply(BigDecimal(conversionRate))
      val convertedNotional = Notional(convertedValue,request.targetCurrency)
      log().info("Converted ${request.notional} -> $convertedNotional with rate of $conversionRate")
      return convertedNotional
   }
}
