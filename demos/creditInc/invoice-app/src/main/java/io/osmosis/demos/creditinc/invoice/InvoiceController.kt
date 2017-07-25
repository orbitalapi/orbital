package io.osmosis.demos.creditinc.invoice

import io.osmosis.polymer.Polymer
import io.osmosis.polymer.models.json.addAnnotatedInstance
import lang.taxi.annotations.DataType
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate


@DataType("polymer.creditInc.Client")
data class Client(@DataType("polymer.creditInc.ClientId") val clientId: String)

@DataType("polymer.creditInc.Money")
data class Money(
   @DataType("polymer.creditInc.Currency") val currency: String,
   @DataType("polymer.creditInc.MoneyAmount") val amount: BigDecimal)

@DataType("polymer.creditInc.Invoice")
data class Invoice(
   @DataType("polymer.creditInc.clientId")
   val clientId: String,
   @DataType("polymer.creditInc.settlementDate")
   val settlementDate: LocalDate,
   @DataType("polymer.creditInc.Money")
   val amount: Money
)


@RestController
class InvoiceController(val polymer: Polymer) {

   @RequestMapping(method = arrayOf(RequestMethod.POST), value = "/creditMarkup")
   fun calculateCreditMarkup(@RequestBody invoice: Invoice): Money {
      val queryEngine = polymer.query()
      queryEngine.addAnnotatedInstance(invoice)
      val queryResult = queryEngine.find("polymer.creditInc.CreditMarkup")
      TODO()
   }
}
