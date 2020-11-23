package io.vyne.models.functions.stdlib

import com.winterbe.expekt.should
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.testVyne
import org.junit.Test

class StringsTest {
   @Test
   fun concatShouldJoinFieldValues() {
      val schema= TaxiSchema.from("""
         model Person {
            firstName : String
            lastName : String
            fullName : String by concat(this.firstName, ' ',this.lastName)
         }
      """.trimIndent())
      val json = """{
         |"firstName" : "Jimmy",
         |"lastName" : "Spitts"
         |}
      """.trimMargin()
      val person = TypedInstance.from(schema.type("Person"), json, schema, source = Provided) as TypedObject
      person["fullName"].value.should.equal("Jimmy Spitts")
   }

   @Test
   fun concatShouldIgnoreTypedNulls() {
      val schema= TaxiSchema.from("""
         model Person {
            firstName : String
            lastName : String
            fullName : String by concat(this.firstName, ' ',this.lastName)
         }
      """.trimIndent())
      val json = """{
         |"firstName" : "Jimmy",
         |"lastName" : null
         |}
      """.trimMargin()
      val person = TypedInstance.from(schema.type("Person"), json, schema, source = Provided) as TypedObject
      person["fullName"].value.should.equal("Jimmy ")
   }

   @Test
   fun `trim should trim whitespace`() {
      val schema= TaxiSchema.from("""
         model Person {
            firstName : String
            lastName : String
            fullName : String by trim(concat(this.firstName, ' ',this.lastName))
         }
      """.trimIndent())
      val json = """{
         |"firstName" : " Jimmy ",
         |"lastName" : null
         |}
      """.trimMargin()
      val person = TypedInstance.from(schema.type("Person"), json, schema, source = Provided) as TypedObject
      person["fullName"].value.should.equal("Jimmy")
   }

   @Test
   fun leftShouldReturnSubstring() {
      val schema= TaxiSchema.from("""
         model Person {
            fullName : String
            title : String by left(this.fullName, 3)
         }
      """.trimIndent())
      val json = """{
         |"fullName" : "Mr. Jimmy",
         |"lastName" : "Spitts"
         |}
      """.trimMargin()
      val person = TypedInstance.from(schema.type("Person"), json, schema, source = Provided) as TypedObject
      person["title"].value.should.equal("Mr.")
   }

   @Test
   fun rightShouldReturnRightMostCharacters() {
      val schema = TaxiSchema.from("""
         model Trade {
            symbol : String
            ccy1 : String by upperCase(right(this.symbol,4))
         }
      """.trimIndent())
      val json = """{
         |"symbol" : "gbp/usd"
         |}
      """.trimMargin()
      val trade = TypedInstance.from(schema.type("Trade"), json, schema, source = Provided) as TypedObject
      trade["ccy1"].value.should.equal("USD")
   }

   @Test
   fun midShouldReturnSubstring() {
      val schema = TaxiSchema.from("""
         model Trade {
            symbol : String
            ccy2 : String by upperCase(mid(this.symbol,4,7))
         }
      """.trimIndent())
      val json = """{
         |"symbol" : "gbp/usd foo bar"
         |}
      """.trimMargin()
      val trade = TypedInstance.from(schema.type("Trade"), json, schema, source = Provided) as TypedObject
      trade["ccy2"].value.should.equal("USD")
   }

   @Test
   fun upperShouldConvertToUppercase() {
      val schema = TaxiSchema.from("""
         model Trade {
            symbol : String
            ccy1 : String by upperCase(left(this.symbol,3))
         }
      """.trimIndent())
      val json = """{
         |"symbol" : "gbp/usd"
         |}
      """.trimMargin()
      val trade = TypedInstance.from(schema.type("Trade"), json, schema, source = Provided) as TypedObject
      trade["ccy1"].value.should.equal("GBP")
   }

   @Test
   fun lowerShouldConvertToUppercase() {
      val schema = TaxiSchema.from("""
         model Trade {
            symbol : String
            ccy1 : String by lowerCase(left(this.symbol,3))
         }
      """.trimIndent())
      val json = """{
         |"symbol" : "GBP/USD"
         |}
      """.trimMargin()
      val trade = TypedInstance.from(schema.type("Trade"), json, schema, source = Provided) as TypedObject
      trade["ccy1"].value.should.equal("gbp")
   }

   @Test
   fun `length should return string length and subtract the return value`() {
      val schema = TaxiSchema.from("""
         model Trade {
            symbol : String
            direction : String by right(this.symbol, length(this.symbol) - 3)
         }
      """.trimIndent())
      val json = """{
         |"symbol" : "12345Buy"
         |}
      """.trimMargin()
      val trade = TypedInstance.from(schema.type("Trade"), json, schema, source = Provided) as TypedObject
      trade["direction"].value.should.equal("Buy")
   }

   @Test
   fun `find is supported`() {
      val (vyne, _) = testVyne("""
         model FindModel {
            symbol : String by column(1)
            symbolPrefix: Int by find(column(1), "BOBUY")
            identifierValue : String? by when (this.symbolPrefix) {
               -1 -> column(1)
               else -> right(column(1),length(this.symbol) - 5)
            }
         }""")
      fun csv(symbol: String) = """$symbol"""
      val fooWithSymbol = TypedInstance.from(vyne.type("FindModel"), csv("BOBUY12345"), vyne.schema, source = Provided) as TypedObject
      fooWithSymbol["identifierValue"].value.should.equal("12345")

      val fooWithIsin = TypedInstance.from(vyne.type("FindModel"), csv("1111111"), vyne.schema, source = Provided) as TypedObject
      fooWithIsin["identifierValue"].value.should.equal("1111111")
   }
}
