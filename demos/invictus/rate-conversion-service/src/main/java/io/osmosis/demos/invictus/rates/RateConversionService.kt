package io.osmosis.demos.invictus.rates

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

data class Money(val ccy: String, val amount: BigDecimal)
@RestController
class RateConversionService {

   @GetMapping("/rates/{fromCcy}/{toCcy}")
   fun convertRates(@PathVariable("fromCcy") fromCcy: String,
                    @PathVariable("toCcy") toCcy: String,
                    @RequestParam("amount") amount: BigDecimal): Money {
      val exchangeRate = BigDecimal("1.0345")
      return Money(toCcy, amount.multiply(exchangeRate))
   }
}
