package io.vyne.models

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.firstRawObject
import io.vyne.models.csv.CsvFormatSpec
import io.vyne.testVyne
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
//import io.vyne.testVyne
import org.junit.Test

class AccessorReaderTest {

   @Test
   @Ignore("Destructured objects are not currently supported")
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
      val parsedResult = TypedInstance.from(vyne.schema.type("LegacyTradeNotification"), xml, vyne.schema, source = Provided) as TypedObject
      expect(parsedResult.type.fullyQualifiedName).to.equal("LegacyTradeNotification")
      expect(parsedResult["nearLegNotional"].type.fullyQualifiedName).to.equal("NearLegNotional")

      val notional = parsedResult["nearLegNotional"] as TypedObject
      expect(notional["amount"].value).to.equal(200000.toBigDecimal())
      expect(notional["amount"].type.fullyQualifiedName).to.equal("MoneyAmount")

      expect(notional["currency"].value).to.equal("GBP")
      expect(notional["currency"].type.fullyQualifiedName).to.equal("Currency")
   }

   @Test
   fun `csv with default value returns default`() {
      val (vyne,_) = testVyne("""
         type Person {
            name : String by column(1)
            sleepy : String by default("Yep")
            age : Int by default(30)
         }
      """)
      val instance = TypedInstance.from(vyne.type("Person"), "Jimmy", vyne.schema, source = Provided) as TypedObject
      instance["name"].value.should.equal("Jimmy")
      instance["sleepy"].value.should.equal("Yep")
      instance["age"].value.should.equal(30)
   }
   @Test
   fun `json with default value returns default if no value provided`() {
      val (vyne,_) = testVyne("""
         type Person {
            name : String by jsonPath("/name")
            sleepy : String by default("Yep")
            age : Int by default(30)
         }
      """)
      val sourceJson = """{ "name" : "Jimmy" }"""
      val instance = TypedInstance.from(vyne.type("Person"), sourceJson, vyne.schema, source = Provided) as TypedObject
      instance["name"].value.should.equal("Jimmy")
      instance["sleepy"].value.should.equal("Yep")
      instance["age"].value.should.equal(30)
   }


   // Cask seems to ingest data records as JSONNode, rather than Map<>
   // Add this test to cover it, but need to look at why
   @Test
   fun `json node with default value returns default if no value provided`() {
      val (vyne,_) = testVyne("""
         type Person {
            name : String by jsonPath("/name")
            sleepy : String by default("Yep")
            age : Int by default(30)
         }
      """
      )
      val sourceJson = """{ "name" : "Jimmy" }"""
      val jsonNode = jacksonObjectMapper().readTree(sourceJson)
      val instance = TypedInstance.from(vyne.type("Person"), jsonNode, vyne.schema, source = Provided) as TypedObject
      instance["name"].value.should.equal("Jimmy")
      instance["sleepy"].value.should.equal("Yep")
      instance["age"].value.should.equal(30)
   }

   @Test
   fun `when field with defined default has value then value is used`(): Unit = runBlocking {
      val (vyne, _) = testVyne(
         """
         model Person {
            @Id
            id : PersonId inherits Int by column(1)
            firstName : FirstName inherits String by column(2)
            lastName : LastName inherits String by column(3)
         }

         @io.vyne.formats.Csv(
            delimiter = "|",
            nullValue = "NULL",
            useFieldNamesAsColumnNames = true
         )
         model Target {
            personId : PersonId by default(0)
            givenName : FirstName by default("Jack")
            surname : LastName by default("Spratt")
            age : Age inherits Int by default(23) // Not provided, use the Default
         }"""
      )
      val targetType = vyne.type("Target")
      val typedInstance = TypedInstance.from(
         vyne.type("Person"), "1,Jimmy,Schmitts", vyne.schema, formatSpecs = listOf(
            CsvFormatSpec
         )
      )
      val builtObject = vyne.from(typedInstance).build(targetType.qualifiedName).firstRawObject()
      builtObject.should.equal(
         mapOf(
            "personId" to 1,
            "givenName" to "Jimmy",
            "surname" to "Schmitts",
            "age" to 23
         )
      )
   }
}

