package io.vyne.models.functions.stdlib

import com.winterbe.expekt.should
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test

class StringsTest {
   @Test
   fun concatShouldJoinFieldValues() {
      val schema= TaxiSchema.from("""
         import vyne.stdlib.concat
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
   fun leftShouldReturnSubstring() {
      val schema= TaxiSchema.from("""
         import vyne.stdlib.left
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
         import vyne.stdlib.right
         import vyne.stdlib.upperCase

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
         import vyne.stdlib.mid
         import vyne.stdlib.upperCase

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
         import vyne.stdlib.left
         import vyne.stdlib.upperCase

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
         import vyne.stdlib.left
         import vyne.stdlib.lowerCase

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

}
