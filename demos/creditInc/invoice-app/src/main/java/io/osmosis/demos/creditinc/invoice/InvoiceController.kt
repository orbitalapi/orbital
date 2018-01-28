package io.osmosis.demos.creditinc.invoice

import io.osmosis.polymer.models.json.addAnnotatedInstance
import io.polymer.spring.PolymerFactory
import lang.taxi.annotations.DataType
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate


@DataType("polymer.creditInc.invoicing.Money")
data class Money(
   @field:DataType("polymer.creditInc.Currency") val currency: String,
   // Note: This field intentionally has a different name to polymer.creditInc.Money.
   // This is to demonstrate field names aren't as relevant as types when mapping
   @field:DataType("polymer.creditInc.MoneyAmount") val amount: BigDecimal)

@DataType("polymer.creditInc.Invoice")
data class Invoice(
   @field:DataType("polymer.creditInc.ClientId")
   val clientId: String,
   @field:DataType("polymer.creditInc.settlementDate")
   val settlementDate: LocalDate,
   //   @field:DataType("polymer.creditInc.Money")
   val amount: Money
)


@RestController
class InvoiceController(val polymerFactory: PolymerFactory) {

   @RequestMapping(method = arrayOf(RequestMethod.POST), value = "/creditMarkup")
   fun calculateCreditMarkup(@RequestBody invoice: Invoice): Money {
      val queryEngine = polymerFactory.createPolymer().query()
      queryEngine.addAnnotatedInstance(invoice)
      val queryResult = queryEngine.find("polymer.creditInc.CreditRiskCost")
      TODO()
   }
}
