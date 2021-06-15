package io.vyne.models.functions.stdlib

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import io.vyne.models.ConversionService
import io.vyne.models.EvaluatedExpression
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.models.TypedObject
import io.vyne.models.TypedValue
import io.vyne.schemas.Type
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.testVyne
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.PrimitiveType
import org.junit.Test

class StringsTest {
   val stubAccessor:FunctionAccessor = mock {
      on { asTaxi() } doReturn "Stubbed function"
   }
   @Test
   fun concatShouldJoinFieldValues() {
      val schema = TaxiSchema.from(
         """
         model Person {
            firstName : String
            lastName : String
            fullName : String by concat(this.firstName, ' ',this.lastName)
         }
      """.trimIndent()
      )
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
      val schema = TaxiSchema.from(
         """
         model Person {
            firstName : String
            lastName : String
            fullName : String by concat(this.firstName, ' ',this.lastName)
         }
      """.trimIndent()
      )
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
      val schema = TaxiSchema.from(
         """
         model Person {
            firstName : String
            lastName : String
            fullName : String by trim(concat(this.firstName, ' ',this.lastName))
         }
      """.trimIndent()
      )
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
      val schema = TaxiSchema.from(
         """
         model Person {
            fullName : String
            title : String by left(this.fullName, 3)
         }
      """.trimIndent()
      )
      val json = """{
         |"fullName" : "Mr. Jimmy",
         |"lastName" : "Spitts"
         |}
      """.trimMargin()
      val person = TypedInstance.from(schema.type("Person"), json, schema, source = Provided) as TypedObject
      person["title"].value.should.equal("Mr.")
   }


   @Test
   fun leftShouldReturnNullIfOutOfBounds() {
      val schema = TaxiSchema.from(
         """
         model Person {
            fullName : String
            title : String by left(this.fullName, -1)
         }
      """.trimIndent()
      )
      val json = """{
         |"fullName" : "Mr. Jimmy",
         |"lastName" : "Spitts"
         |}
      """.trimMargin()
      val person = TypedInstance.from(schema.type("Person"), json, schema, source = Provided) as TypedObject
      person["title"].value.should.be.`null`
   }

   @Test
   fun leftShouldFullStringIfGreaterThanLength() {
      val schema = TaxiSchema.from(
         """
         model Person {
            fullName : String
            title : String by left(this.fullName, 100)
         }
      """.trimIndent()
      )
      val json = """{
         |"fullName" : "Mr. Jimmy",
         |"lastName" : "Spitts"
         |}
      """.trimMargin()
      val person = TypedInstance.from(schema.type("Person"), json, schema, source = Provided) as TypedObject
      person["title"].value.should.equal("Mr. Jimmy")
   }

   @Test
   fun rightShouldReturnRightMostCharacters() {
      val schema = TaxiSchema.from(
         """
         model Trade {
            symbol : String
            ccy1 : String by upperCase(right(this.symbol,4))
         }
      """.trimIndent()
      )
      val json = """{
         |"symbol" : "gbp/usd"
         |}
      """.trimMargin()
      val trade = TypedInstance.from(schema.type("Trade"), json, schema, source = Provided) as TypedObject
      trade["ccy1"].value.should.equal("USD")
   }

   @Test
   fun rightShouldReturnNullIfIndexIsOutOfBounds() {
      val schema = TaxiSchema.from(
         """
         model Trade {
            symbol : String
            ccy1 : String by upperCase(right(this.symbol,20))
         }
      """.trimIndent()
      )
      val json = """{
         |"symbol" : "gbp/usd"
         |}
      """.trimMargin()
      val trade = TypedInstance.from(schema.type("Trade"), json, schema, source = Provided) as TypedObject
      trade["ccy1"].value.should.be.`null`
   }


   @Test
   fun midShouldReturnSubstring() {
      val schema = TaxiSchema.from(
         """
         model Trade {
            symbol : String
            ccy2 : String by upperCase(mid(this.symbol,4,7))
         }
      """.trimIndent()
      )
      val json = """{
         |"symbol" : "gbp/usd foo bar"
         |}
      """.trimMargin()
      val trade = TypedInstance.from(schema.type("Trade"), json, schema, source = Provided) as TypedObject
      trade["ccy2"].value.should.equal("USD")
   }

   @Test
   fun midShouldReturnNullIfAnyInputsAreNull() {
      val schema = TaxiSchema.from("")
      val stringType = schema.type(PrimitiveType.STRING)
      val intType = schema.type(PrimitiveType.INTEGER)
      val helloArg = "hello".toTypedValue(stringType)
      val startArg = 2.toTypedValue(intType)
      val endArg = 4.toTypedValue(intType)
      val nullArg = TypedNull.create(stringType)

      Mid.invoke(listOf(nullArg, startArg, endArg), schema, stringType, stubAccessor).value.should.be.`null`
      Mid.invoke(listOf(helloArg, nullArg, endArg), schema, stringType, stubAccessor).value.should.be.`null`
      Mid.invoke(listOf(helloArg, startArg, nullArg), schema, stringType, stubAccessor).value.should.be.`null`
   }

   @Test
   fun midShouldReturnNullIfStartIsOutOfBounds() {
      val schema = TaxiSchema.from(
         """
         model Trade {
            symbol : String
            ccy2 : String by upperCase(mid(this.symbol,10,12))
         }
      """.trimIndent()
      )
      val json = """{
         |"symbol" : "gbp/usd"
         |}
      """.trimMargin()
      val trade = TypedInstance.from(schema.type("Trade"), json, schema, source = Provided) as TypedObject
      trade["ccy2"].value.should.be.`null`
   }

   @Test
   fun shouldCaptureTheLineageOfInputsInResult() {
      val schema = TaxiSchema.from("""
         model Person {
            firstName :  FirstName inherits String
            lastName  : LastName inherits String
            fullName  : String by concat(this.firstName, ' ', this.lastName)
         }
      """.trimIndent())
      val json = """{ "firstName" : "Jimmy" , "lastName" : "Schmitts" }"""
      val person = TypedInstance.from(schema.type("Person"), json, schema, source = Provided) as TypedObject
      val source = person["fullName"].source as EvaluatedExpression
      source.inputs.should.have.size(3)
      source.inputs[0].should.equal(person["firstName"])
      source.inputs[2].should.equal(person["lastName"])
      val stringInput = source.inputs[1] as TypedValue
      stringInput.source.should.equal(Provided)
      stringInput.value.should.equal(" ")
      stringInput.typeName.should.equal(PrimitiveType.STRING.qualifiedName)
   }

   @Test
   fun upperShouldConvertToUppercase() {
      val schema = TaxiSchema.from(
         """
         model Trade {
            symbol : String
            ccy1 : String by upperCase(left(this.symbol,3))
         }
      """.trimIndent()
      )
      val json = """{
         |"symbol" : "gbp/usd"
         |}
      """.trimMargin()
      val trade = TypedInstance.from(schema.type("Trade"), json, schema, source = Provided) as TypedObject
      trade["ccy1"].value.should.equal("GBP")
   }

   @Test
   fun upperShouldReturnNullIfPassedNull() {
      val schema = TaxiSchema.from(
         """
         model Trade {
            symbol : String
            ccy1 : String by upperCase(this.symbol)
         }
      """.trimIndent()
      )
      val json = """{
         |"symbol" : null
         |}
      """.trimMargin()
      val trade = TypedInstance.from(schema.type("Trade"), json, schema, source = Provided) as TypedObject
      trade["ccy1"].value.should.be.`null`
   }

   @Test
   fun lowerShouldConvertToUppercase() {
      val schema = TaxiSchema.from(
         """
         model Trade {
            symbol : String
            ccy1 : String by lowerCase(left(this.symbol,3))
         }
      """.trimIndent()
      )
      val json = """{
         |"symbol" : "GBP/USD"
         |}
      """.trimMargin()
      val trade = TypedInstance.from(schema.type("Trade"), json, schema, source = Provided) as TypedObject
      trade["ccy1"].value.should.equal("gbp")
   }

   @Test
   fun lowerShouldReturnNullIfPassedNull() {
      val schema = TaxiSchema.from(
         """
         model Trade {
            symbol : String
            ccy1 : String by lowerCase(this.symbol)
         }
      """.trimIndent()
      )
      val json = """{
         |"symbol" : null
         |}
      """.trimMargin()
      val trade = TypedInstance.from(schema.type("Trade"), json, schema, source = Provided) as TypedObject
      trade["ccy1"].value.should.be.`null`
   }

   @Test
   fun `length should return string length and subtract the return value`() {
      val schema = TaxiSchema.from(
         """
         model Trade {
            symbol : String
            direction : String by right(this.symbol, length(this.symbol) - 3)
         }
      """.trimIndent()
      )
      val json = """{
         |"symbol" : "12345Buy"
         |}
      """.trimMargin()
      val trade = TypedInstance.from(schema.type("Trade"), json, schema, source = Provided) as TypedObject
      trade["direction"].value.should.equal("Buy")
   }

   @Test
   fun `length should return null from null`() {

   }

   @Test
   fun `find is supported`() {
      val (vyne, _) = testVyne(
         """
         model FindModel {
            symbol : String by column(1)
            symbolPrefix: Int by indexOf(column(1), "BOBUY")
            identifierValue : String? by when (this.symbolPrefix) {
               -1 -> column(1)
               else -> right(column(1),length(this.symbol) - 5)
            }
         }"""
      )
      fun csv(symbol: String) = """$symbol"""
      val fooWithSymbol =
         TypedInstance.from(vyne.type("FindModel"), csv("BOBUY12345"), vyne.schema, source = Provided) as TypedObject
      fooWithSymbol["identifierValue"].value.should.equal("12345")

      val fooWithIsin =
         TypedInstance.from(vyne.type("FindModel"), csv("1111111"), vyne.schema, source = Provided) as TypedObject
      fooWithIsin["identifierValue"].value.should.equal("1111111")
      fooWithIsin["identifierValue"].typeName.should.equal("lang.taxi.String")
   }

   @Test
   fun `concat should yield correct return type`() {
      val schema = TaxiSchema.from(
         """
         type ResetFrequencyStr inherits String
         type ResetLength inherits Int
         type ResetTerm inherits String

         model SomeModel {
            attReferenceRateTermValue : ResetLength?
            attReferenceRateTermUnit : ResetTerm?
            attResetFrequencyStr : ResetFrequencyStr? by concat (this.attReferenceRateTermValue, this.attReferenceRateTermUnit)
         }
      """.trimIndent()
      )
      val json = """{
         |"attReferenceRateTermValue" : 3,
         |"attReferenceRateTermUnit" : "Month"
         |}
      """.trimMargin()
      val person = TypedInstance.from(schema.type("SomeModel"), json, schema, source = Provided) as TypedObject
      person["attResetFrequencyStr"].value.should.equal("3Month")
      person["attResetFrequencyStr"].typeName.should.equal("ResetFrequencyStr")
   }

   @Test
   fun `enum value derivation via right`() {
      val schema = TaxiSchema.from(
         """
         enum CurrencyCode {
               USD,
               EUR
         }
         model Trade {
            underlying: String?
            quantityCurrency: CurrencyCode by right(underlying,4)
         }
      """.trimIndent()
      )
      val json = """{
         |"underlying" : "EUR/USD"
         |}
      """.trimMargin()
      val trade = TypedInstance.from(schema.type("Trade"), json, schema, source = Provided) as TypedObject
      trade["quantityCurrency"].value.should.equal("USD")
   }
}

fun Any?.toTypedValue(type: Type): TypedInstance {
   return when (this) {
      null -> TypedNull.create(type)
      else -> TypedValue.from(type, this, ConversionService.DEFAULT_CONVERTER, source = Provided)
   }
}
