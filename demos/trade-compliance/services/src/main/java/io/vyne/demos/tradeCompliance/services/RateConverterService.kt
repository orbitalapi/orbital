package io.vyne.demos.tradeCompliance.services

import io.vyne.tradeCompliance.Currency
import io.vyne.tradeCompliance.TradeValue
import lang.taxi.annotations.Operation
import lang.taxi.annotations.ResponseConstraint
import lang.taxi.annotations.ResponseContract
import lang.taxi.annotations.Service
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.math.RoundingMode

@RestController
@Service
class RateConverterService {

   private val rates = listOf(
      FxRate(base = "USD", target = "EUR", rate = (0.86).toBigDecimal()),
      FxRate(base = "GBP", target = "EUR", rate = (1.11).toBigDecimal()),
      FxRate(base = "EUR", target = "GBP", rate = (0.9).toBigDecimal()),
      FxRate(base = "GBP", target = "USD", rate = (1.29).toBigDecimal())
   )

   @PostMapping("/tradeValues/{targetCurrency}")
   @ResponseContract(basedOn = "source",
      constraints = ResponseConstraint("currency = targetCurrency")
   )
   @Operation
   fun convert(@PathVariable("targetCurrency") targetCurrency: Currency, @RequestBody source: TradeValue): TradeValue {
      val rate = this.rates.firstOrNull {
         it.base == source.currency && it.target == targetCurrency
      } ?: error("No conversion rate defined from ${source.currency} to $targetCurrency")

      return TradeValue(
         targetCurrency,
         source.value.multiply(rate.rate).setScale(5, RoundingMode.HALF_EVEN)
      )
   }
}

data class FxRate(val base: Currency, val target: Currency, val rate: BigDecimal)
