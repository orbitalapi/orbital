package io.vyne.query

import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.TestSchema
import io.vyne.models.json.addJsonModel
import io.vyne.formulas.CalculatorRegistry
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.testVyne
import org.junit.Test
import java.math.BigDecimal

class CalculatedFieldScanStrategyTest {
   val taxiDef = """
namespace vyne.example

type QtyTick inherits Decimal
type QtyHit inherits Decimal
type MarketOrderId inherits String
type BankOrderId inherits String
type SecondaryOrderId inherits String

type Order {
   qtyTick: QtyTick?
   qtyHit: QtyHit?
   bankOrderId: BankOrderId?
   marketOrderId: MarketOrderId?
   secondaryOrderId: SecondaryOrderId?
}

type Invoice {
   qtyTot: Decimal as (QtyTick * QtyHit)
   orderId: String as coalesce(vyne.example.MarketOrderId, BankOrderId, SecondaryOrderId)
}

"""
   val testSchema = TaxiSchema.from(taxiDef)
   val vyne = TestSchema.vyne(QueryEngineFactory.default(), TaxiSchema.from(taxiDef))

   @Test
   fun `Given operands available in context calculated field value is set`() {
      val json = """
            {
               "qtyTick" : "2",
               "qtyHit" : "200",
               "bankOrderId": "bankOrderId"
            }
      """.trimIndent()
      vyne.addJsonModel("vyne.example.Order", json)
      val qtyTot = (vyne.type("vyne.example.Invoice").attributes["qtyTot"] ?: error("")).type
      val result = CalculatedFieldScanStrategy(CalculatorRegistry()).invoke(TestSchema.typeNode(qtyTot.fullyQualifiedName, QueryParser(testSchema)), vyne.query())
      expect(result.matchedNodes).size.to.equal(1)
      expect(result.matchedNodes.entries.first().key.type.name.fullyQualifiedName).to.equal(qtyTot.fullyQualifiedName)
      expect(result.matchedNodes.entries.first().value!!.value).to.equal(BigDecimal("400"))

      val orderId = (vyne.type("vyne.example.Invoice").attributes["orderId"] ?: error("")).type
      val orderIdResult = CalculatedFieldScanStrategy(CalculatorRegistry()).invoke(TestSchema.typeNode(orderId.fullyQualifiedName, QueryParser(testSchema)), vyne.query())
      expect(orderIdResult.matchedNodes).size.to.equal(1)
      expect(orderIdResult.matchedNodes.entries.first().key.type.name.fullyQualifiedName).to.equal(orderId.fullyQualifiedName)
      expect(orderIdResult.matchedNodes.entries.first().value!!.value).to.equal("bankOrderId")
   }

   @Test
   fun `Calculations can handle null values appropriately`() {
      val json = """
            {
               "qtyTick" : "2",
               "bankOrderId": "bankOrderId",
               "marketOrderId": "marketOrderId"
            }
      """.trimIndent()
      vyne.addJsonModel("vyne.example.Order", json)
      val qtyTot = (vyne.type("vyne.example.Invoice").attributes["qtyTot"] ?: error("")).type
      val result = CalculatedFieldScanStrategy(CalculatorRegistry()).invoke(TestSchema.typeNode(qtyTot.fullyQualifiedName, QueryParser(testSchema)), vyne.query())
      expect(result.matchedNodes).size.to.equal(0)
      val orderId = (vyne.type("vyne.example.Invoice").attributes["orderId"] ?: error("")).type
      val orderIdResult = CalculatedFieldScanStrategy(CalculatorRegistry()).invoke(TestSchema.typeNode(orderId.fullyQualifiedName, QueryParser(testSchema)), vyne.query())
      expect(orderIdResult.matchedNodes).size.to.equal(1)
      expect(orderIdResult.matchedNodes.entries.first().key.type.name.fullyQualifiedName).to.equal(orderId.fullyQualifiedName)
      expect(orderIdResult.matchedNodes.entries.first().value!!.value).to.equal("marketOrderId")

   }

   @Test
   fun `Can concatenate Date and Time through calculated field`() {
      val schema = """
         type TransactionTime inherits Time
         type TransactionDate inherits Date
         type TransactionDateTime inherits Instant

         model Transaction {
            date : TransactionDate
            time : TransactionTime
            timestamp : TransactionDateTime as (TransactionDate + TransactionTime)
         }
         """
      val (vyne, _) = testVyne(schema)
      val json = """
         {
            "date" : "2020-10-12",
            "time" : "18:00:00"
         }
      """.trimIndent()
      vyne.addJsonModel("Transaction", json)
      val result = vyne.query().find("TransactionDateTime")
      result.resultMap["TransactionDateTime"]!!.should.equal("2020-10-12T18:00:00.000Z")
   }


}
