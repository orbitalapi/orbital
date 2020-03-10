package io.vyne.models

import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.testVyne
import org.junit.Assert.*
import org.junit.Test

class AccessorReaderTest {

   @Test
   fun canParseTypedObjectWhenAccessorIsDeclared() {
      val src = """
type Money {
   amount : MoneyAmount as Decimal
   currency : Currency as String
}
type alias Instrument as String
type NearLegNotional inherits Money {}
type FarLegNotional inherits Money {}

type LegacyTradeNotification {
   nearLegNotional : NearLegNotional {
       amount by xpath("/tradeNotification/legs/leg[1]/notional/amount/text()")
       currency by xpath("/tradeNotification/legs/leg[1]/notional/currency/text()")
   }
   farLegNotional : FarLegNotional {
       amount by xpath("/tradeNotification/legs/leg[2]/notional/amount/text()")
       currency by xpath("/tradeNotification/legs/leg[2]/notional/currency/text()")
   }
}
        """.trimIndent()
      val (vyne, _) = testVyne(src)
      val xml = """
 <tradeNotification>
    <legs>
        <leg>
            <notional>
                <amount>200000</amount>
                <currency>GBP</currency>
            </notional>
        </leg>
        <leg>
            <notional>
                <amount>700000</amount>
                <currency>GBP</currency>
            </notional>
        </leg>
    </legs>
</tradeNotification>
      """.trimIndent()
      val parsedResult = TypedInstance.from(vyne.schema.type("LegacyTradeNotification"), xml, vyne.schema) as TypedObject
      expect(parsedResult.type.fullyQualifiedName).to.equal("LegacyTradeNotification")
      expect(parsedResult["nearLegNotional"].type.fullyQualifiedName).to.equal("NearLegNotional")

      val notional = parsedResult["nearLegNotional"] as TypedObject
      expect(notional["amount"].value).to.equal(200000.toBigDecimal())
      expect(notional["amount"].type.fullyQualifiedName).to.equal("MoneyAmount")

      expect(notional["currency"].value).to.equal("GBP")
      expect(notional["currency"].type.fullyQualifiedName).to.equal("Currency")
   }

   @Test
   fun canReadCsvData() {
      val src = """type alias FirstName as String
type alias LastName as String
type Person {
   firstName : FirstName by column(0)
   lastName : LastName by column(1)
}
"""
      val (vyne, _) = testVyne(src)
      val csv = "firstName,lastName\n" +
         "jimmy,parsons"
      val parsedResult = TypedInstance.from(vyne.schema.type("Person"), csv, vyne.schema) as TypedObject
      expect(parsedResult.type.fullyQualifiedName).to.equal("Person")
      parsedResult["firstName"].value.should.equal("jimmy")
   }

   @Test
   fun canReadCsvDataWithMultipleRecords() {
      val src = """type alias FirstName as String
type alias LastName as String
type Person {
   firstName : FirstName by column(0)
   lastName : LastName by column(1)
}

@CsvList
type alias PersonList as Person[]
"""
      val (vyne, _) = testVyne(src)
      val csv = "firstName,lastName\n" +
         "jimmy,parsons\n" +
         "olly,spurrs"
      val parsedResult = TypedInstance.from(vyne.schema.type("PersonList"), csv, vyne.schema)
      TODO()
   }
}
