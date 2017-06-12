package io.osmosis.demos.invictus.credit

import io.osmosis.DataAttribute
import io.osmosis.DataFormat
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

data class CreditCostRequest(
   @DataAttribute("invictus.invoiceValue", format = DataFormat("invictus.currency.GBP"))
   val invoiceValue: BigDecimal,
   @DataAttribute("invictus.industryCode", format = DataFormat("uk.sic2003"))
   val industryCode: Int)

data class CreditCostResponse(
   // Scenarios to consider:
   // What is this was a joda Money type?
   // What if there were two fields, value & ccy?  How do we indicate the two are related?
   @DataAttribute("invictus.invoiceValue", format = DataFormat("invictus.currency.GBP"))
   val cost: BigDecimal
)

@RestController
@RequestMapping("/costs")
class CreditCostService {

   @PostMapping
   fun calculateCreditCosts(@RequestBody request: CreditCostRequest): CreditCostResponse {
      return CreditCostResponse(BigDecimal("0.05"))
   }
}
