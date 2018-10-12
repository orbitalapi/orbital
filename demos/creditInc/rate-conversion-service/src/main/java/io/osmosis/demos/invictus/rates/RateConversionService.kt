package io.osmosis.demos.invictus.rates

import lang.taxi.annotations.*
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@DataType("vyne.creditInc.Money")
data class Money(
   @field:DataType("vyne.creditInc.Currency") val currency: String,
   @field:DataType("vyne.creditInc.MoneyAmount") val value: BigDecimal)


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

   @PostMapping("/rates/{targetCcy}")
   @Operation
   @ResponseContract(basedOn = "source",
      constraints = ResponseConstraint("currency = targetCurrency")
   )
   fun convertRates(@RequestBody source: Money, @DataType("vyne.creditInc.Currency") @PathVariable("targetCcy") targetCurrency: String): Money {
      val exchangeRate = BigDecimal("1.0345")
      return Money(targetCurrency, source.value.multiply(exchangeRate))
   }
}
