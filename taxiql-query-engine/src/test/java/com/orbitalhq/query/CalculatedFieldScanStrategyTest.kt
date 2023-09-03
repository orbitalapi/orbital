package com.orbitalhq.query

import com.winterbe.expekt.expect
import com.winterbe.expekt.should
//import com.orbitalhq.TestSchema
import com.orbitalhq.models.json.addJsonModel
import com.orbitalhq.formulas.CalculatorRegistry
import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedInstance
//import com.orbitalhq.testVyne
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.math.BigDecimal

/*
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

   @Test
   fun `given coalesce refers to field names then it is calculated`() {
      val (vyne, _) = testVyne("""
         type PreferredName inherits String
         model Person {
            firstName : FirstName as String
            nickName : NickName as String
            preferredName : PreferredName as coalesce(NickName, FirstName)
         }
      """.trimIndent())
      val json = """{ "firstName" : "Marty" } """
      val withoutNickName = TypedInstance.from(vyne.type("Person"), json, vyne.schema, source = Provided)
      val withoutNickNameResult = runBlocking {vyne.query(additionalFacts = setOf(withoutNickName)).find("PreferredName")}
      withoutNickNameResult["PreferredName"]?.value.should.equal("Marty")

      val withNicknameJson = """{ "firstName" : "Marty" , "nickName" : "Jimmy" } """
      val withNickName = TypedInstance.from(vyne.type("Person"), withNicknameJson, vyne.schema, source = Provided)
      val withNickNameResult = runBlocking {vyne.query(additionalFacts = setOf(withNickName)).find("PreferredName")}
      withNickNameResult["PreferredName"]?.value.should.equal("Jimmy")
   }

   @Test
   fun `Given operands available in context calculated field value is set`() {
      val (vyne, _) = testVyne(taxiDef)

      val json = """
            {
               "qtyTick" : "2",
               "qtyHit" : "200",
               "bankOrderId": "bankOrderId"
            }
      """.trimIndent()
      vyne.addJsonModel("vyne.example.Order", json)
      val qtyTot = (vyne.type("vyne.example.Invoice").attributes["qtyTot"] ?: error("")).type
      val result = runBlocking {CalculatedFieldScanStrategy(CalculatorRegistry()).invoke(TestSchema.typeNode(qtyTot.fullyQualifiedName, QueryParser(vyne.schema)), vyne.query(), InvocationConstraints.withAlwaysGoodPredicate)}
      expect(result.matchedNodes).size.to.equal(1)
      expect(result.matchedNodes.entries.first().key.type.name.fullyQualifiedName).to.equal(qtyTot.fullyQualifiedName)
      expect(result.matchedNodes.entries.first().value!!.value).to.equal(BigDecimal("400"))

      val orderId = (vyne.type("vyne.example.Invoice").attributes["orderId"] ?: error("")).type
      val orderIdResult = runBlocking {CalculatedFieldScanStrategy(CalculatorRegistry()).invoke(TestSchema.typeNode(orderId.fullyQualifiedName, QueryParser(vyne.schema)), vyne.query(), InvocationConstraints.withAlwaysGoodPredicate)}
      expect(orderIdResult.matchedNodes).size.to.equal(1)
      expect(orderIdResult.matchedNodes.entries.first().key.type.name.fullyQualifiedName).to.equal(orderId.fullyQualifiedName)
      expect(orderIdResult.matchedNodes.entries.first().value!!.value).to.equal("bankOrderId")
   }

   @Test
   fun `Calculations can handle null values appropriately`() {
      val (vyne, _) = testVyne(taxiDef)

      val json = """
            {
               "qtyTick" : "2",
               "bankOrderId": "bankOrderId",
               "marketOrderId": "marketOrderId"
            }
      """.trimIndent()
      vyne.addJsonModel("vyne.example.Order", json)
      val qtyTot = (vyne.type("vyne.example.Invoice").attributes["qtyTot"] ?: error("")).type
      val result = runBlocking {CalculatedFieldScanStrategy(CalculatorRegistry()).invoke(TestSchema.typeNode(qtyTot.fullyQualifiedName, QueryParser(vyne.schema)), vyne.query(), InvocationConstraints.withAlwaysGoodPredicate)}
      expect(result.matchedNodes).size.to.equal(0)
      val orderId = (vyne.type("vyne.example.Invoice").attributes["orderId"] ?: error("")).type
      val orderIdResult = runBlocking {CalculatedFieldScanStrategy(CalculatorRegistry()).invoke(TestSchema.typeNode(orderId.fullyQualifiedName, QueryParser(vyne.schema)), vyne.query(), InvocationConstraints.withAlwaysGoodPredicate)}
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
      val result = runBlocking {vyne.query().find("TransactionDateTime")}
      result.resultMap["TransactionDateTime"]!!.should.equal("2020-10-12T18:00:00.000Z")
   }


}
*/