package io.osmosis.demos.invictus.credit

import lang.taxi.annotations.*
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@DataType("polymer.creditInc.Money")
@ParameterType
data class Money(
   @field:DataType("polymer.creditInc.Currency") val currency: String,
   @field:DataType("polymer.creditInc.MoneyAmount") val value: BigDecimal)

@DataType("polymer.creditInc.CreditCostRequest")
@ParameterType
data class CreditCostRequest(
   @field:DataType
   val invoiceValue: Money,

   @field:DataType("isic.uk.SIC2008")
   val industryCode: Int
)
//data class CreditCostRequest(
// TODO : We should support this type of request object,
// but we don't -- yet -- as we can't express a constraint on one field
// as a dependency on another (in this case, the invoiceValue must've come
// from something with GBP
// There is a gitlab issue for this, but I can't find it, because I'm on a train.
//   @DataType("invictus.invoiceValue")
//   @DataFormat("invictus.currency.GBP")
//   val invoiceValue: BigDecimal,
//
//   @DataType("invictus.industryCode.sic2003")
//   val industryCode: Int)

@DataType("polymer.creditInc.CreditCostResponse")
data class CreditCostResponse(
   // Scenarios to consider:
   // What is this was a joda Money type?
   // What if there were two fields, value & ccy?  How do we indicate the two are related?
   @field:DataType("polymer.creditInc.CreditRiskCost")
   val cost: BigDecimal
)

@RestController
@RequestMapping("/costs")
@Service("CreditCostService")
@Namespace("polymer.creditInc.creditMarkup")
class CreditCostService {

   @PostMapping
   @Operation
   fun calculateCreditCosts(@RequestBody request: CreditCostRequest): CreditCostResponse {
      return CreditCostResponse(BigDecimal("0.05"))
   }

}
