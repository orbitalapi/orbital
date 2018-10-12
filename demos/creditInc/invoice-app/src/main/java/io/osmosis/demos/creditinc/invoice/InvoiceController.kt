package io.osmosis.demos.creditinc.invoice

import io.vyne.models.json.addAnnotatedInstance
import io.vyne.spring.VyneFactory
import lang.taxi.annotations.DataType
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate


@DataType("vyne.creditInc.invoicing.Money")
data class Money(
   @field:DataType("vyne.creditInc.Currency") val currency: String,
   // Note: This field intentionally has a different name to vyne.creditInc.Money.
   // This is to demonstrate field names aren't as relevant as types when mapping
   @field:DataType("vyne.creditInc.MoneyAmount") val amount: BigDecimal)

@DataType("vyne.creditInc.Invoice")
data class Invoice(
   @field:DataType("vyne.creditInc.ClientId")
   val clientId: String,
   @field:DataType("vyne.creditInc.settlementDate")
   val settlementDate: LocalDate,
   //   @field:DataType("vyne.creditInc.Money")
   val amount: Money
)


@RestController
class InvoiceController(val vyneFactory: VyneFactory) {

   @RequestMapping(method = arrayOf(RequestMethod.POST), value = "/creditMarkup")
   fun calculateCreditMarkup(@RequestBody invoice: Invoice): Money {
      val queryEngine = vyneFactory.createVyne().query()
      queryEngine.addAnnotatedInstance(invoice)
      val queryResult = queryEngine.find("vyne.creditInc.CreditRiskCost")
      TODO()
   }
}
