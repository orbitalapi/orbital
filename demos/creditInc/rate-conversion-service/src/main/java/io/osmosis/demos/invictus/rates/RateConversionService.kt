package io.osmosis.demos.invictus.rates

import lang.taxi.annotations.*
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@DataType("polymer.creditInc.Money")
data class Money(
   @field:DataType("polymer.creditInc.Currency") val currency: String,
   @field:DataType("polymer.creditInc.MoneyAmount") val value: BigDecimal)


@Service
@RestController
class RateConversionService {

//   @GetMapping("/rates/{fromCcy}/{toCcy}")
//   fun convertRates(@PathVariable("fromCcy") fromCcy: String,
//                    @PathVariable("toCcy") toCcy: String,
//                    @RequestParam("amount") amount: BigDecimal): Money {
//      val exchangeRate = BigDecimal("1.0345")
//      return Money(toCcy, amount.multiply(exchangeRate))
//   }

   @PostMapping("/rates")
   @Operation
   @ResponseContract(basedOn = "source",
      constraints = ResponseConstraint("currency = targetCurrency")
   )
   fun convertRates(source: Money, @DataType("polymer.creditInc.Currency") targetCurrency: String): Money {
      val exchangeRate = BigDecimal("1.0345")
      return Money(targetCurrency, source.value.multiply(exchangeRate))
   }
}
