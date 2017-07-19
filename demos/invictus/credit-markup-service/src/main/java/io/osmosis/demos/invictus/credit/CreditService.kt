package io.osmosis.demos.invictus.credit

import lang.taxi.DataFormat
import lang.taxi.DataType
import lang.taxi.Operation
import lang.taxi.Service
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

data class CreditCostRequest(
   @DataType("invictus.invoiceValue")
   @DataFormat("invictus.currency.GBP")
   val invoiceValue: BigDecimal,

   @DataType("invictus.industryCode.sic2003")
   val industryCode: Int)

data class CreditCostResponse(
   // Scenarios to consider:
   // What is this was a joda Money type?
   // What if there were two fields, value & ccy?  How do we indicate the two are related?
   @DataType("invictus.invoiceValue")
   // I'm not sure if this is a good use of DataFormat.
   @DataFormat("invictus.currency.GBP")
   val cost: BigDecimal
)

@RestController
@RequestMapping("/costs")
@Service
class CreditCostService {

   @PostMapping
   @Operation
   fun calculateCreditCosts(@RequestBody request: CreditCostRequest): CreditCostResponse {
      return CreditCostResponse(BigDecimal("0.05"))
   }
}
