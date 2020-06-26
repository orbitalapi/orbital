package io.vyne.models

import com.winterbe.expekt.should
import io.vyne.testVyne
import org.junit.Test

class ConditionalFieldReaderTest {
   val schema = """
type alias CurrencySymbol as String
type Money {
   quantity : Decimal
   currency : CurrencySymbol
}
type alias CounterpartyId as String

type DealtAmount inherits Money // hehehehe
type SettlementAmount inherits Money
type TradeRecord {
    // define simple properties by xpath expressions
    counterparty : CounterpartyId by xpath("//counterpartyId")
    ccy1 : CurrencySymbol by xpath("//baseAmount/@ccy")
    ccy2 : CurrencySymbol by xpath("//termAmount/@ccy")
    (   dealtAmount : DealtAmount
        settlementAmount : SettlementAmount
    ) by when( xpath("//rate/@ccy") : CurrencySymbol) {
        ccy1 -> {
            dealtAmount(
                quantity by xpath("//baseAmount")
                currency = ccy1
            )
            settlementAmount(
                quantity by xpath("//termAmount")
                currency = ccy2
            )
        }
        ccy2 -> {
            dealtAmount(
                quantity by xpath("//termAmount")
                currency = ccy2
            )
            settlementAmount(
                quantity by xpath("//baseAmount")
                currency = ccy1
            )
        }
    }
}

type TransformedTradeRecord {
   counterpartyId : CounterpartyId
   dealtAmount : DealtAmount
}
      """.trimIndent()

   @Test
   fun canReadAnObjectWithConditionalFields() {

      val (vyne, _) = testVyne(schema)
      val xml = """
<tradeRecord>
    <counterpartyId>client001</counterpartyId>
    <baseAmount ccy="GBP">1000000</baseAmount>
    <termAmount ccy="USD">800000</termAmount>
    <rate ccy="GBP">0.8000</rate>
</tradeRecord>
      """.trimIndent()
      val tradeRecord = TypedInstance.from(vyne.schema.type("TradeRecord"), xml, vyne.schema, source = Provided) as TypedObject
      tradeRecord.type.fullyQualifiedName.should.equal("TradeRecord")
      tradeRecord["ccy1"].value.should.equal("GBP")
      tradeRecord["ccy2"].value.should.equal("USD")
      val dealtAmount = tradeRecord["dealtAmount"] as TypedObject
      dealtAmount.type.fullyQualifiedName.should.equal("DealtAmount")
      dealtAmount["quantity"].type.fullyQualifiedName.should.equal("lang.taxi.Decimal")
      dealtAmount["quantity"].value.should.equal(1_000_000.toBigDecimal())
      dealtAmount["currency"].type.fullyQualifiedName.should.equal("CurrencySymbol")
      dealtAmount["currency"].value.should.equal("GBP")

      val settlementAmount = tradeRecord["settlementAmount"] as TypedObject
      settlementAmount.type.fullyQualifiedName.should.equal("SettlementAmount")
      settlementAmount["quantity"].type.fullyQualifiedName.should.equal("lang.taxi.Decimal")
      settlementAmount["quantity"].value.should.equal(800_000.toBigDecimal())
      settlementAmount["currency"].type.fullyQualifiedName.should.equal("CurrencySymbol")
      settlementAmount["currency"].value.should.equal("USD")
   }

   @Test
   fun canTransformUsingConditionalDataAsSource() {
      val (vyne, _) = testVyne(schema)
      val xml = """
<tradeRecord>
    <counterpartyId>client001</counterpartyId>
    <baseAmount ccy="GBP">1000000</baseAmount>
    <termAmount ccy="USD">800000</termAmount>
    <rate ccy="GBP">0.8000</rate>
</tradeRecord>
      """.trimIndent()
      val tradeRecord = TypedInstance.from(vyne.schema.type("TradeRecord"), xml, vyne.schema, source = Provided) as TypedObject
      val queryContext = vyne.query()
      queryContext.addFact(tradeRecord)
      val result = queryContext.build("TransformedTradeRecord")
      result.isFullyResolved.should.be.`true`
   }
}
