package io.vyne

import com.winterbe.expekt.should
import io.vyne.models.*
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import java.time.Instant


// Note - this tests a class in vyne-core-types.  Testing there is awkward because of lack of access to taxi-schema
class PrimitiveParserTest {
   @Rule
   @JvmField
   val exception = ExpectedException.none()

   @Test
   fun canParseEnums() {
      val src = """
enum Country {
   NZ("New Zealand"),
   AUS("Australia")
}
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      val enum = TypedInstance.from(schema.type("Country"), "NZ", schema) as TypedEnumValue
      enum.type.name.fullyQualifiedName.should.equal("Country")
      enum.name.should.equal("NZ")
   }

   @Test
   fun canParseEnumWithValues() {
      val src = """
enum Country {
   NZ("New Zealand"),
   AUS("Australia")
}
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      val enum = TypedInstance.from(schema.type("Country"), "New Zealand", schema)
      enum.type.name.fullyQualifiedName.should.equal("Country")
      enum.value.should.equal("New Zealand")
   }

   @Test
   fun `can parse enums with Int Values`() {
      val src = """
enum City {
   IZMIR(35),
   ANKARA(6)
}
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      val enum = TypedInstance.from(schema.type("City"), 35, schema, source = Provided)
      enum.type.name.fullyQualifiedName.should.equal("City")
      enum.value.should.equal(35)
   }

   @Test
   fun `can parse enums with boolean names`() {
      val schema = TaxiSchema.from(
         """
         enum IsAlive {
            `true`,
            `false`
         }
      """.trimIndent()
      )
      val enum =TypedInstance.from(schema.type("IsAlive"), true, schema)
      enum.type.name.fullyQualifiedName.should.equal("IsAlive")
      // Note -it's a string, because enum values are string by default
      enum.value.should.equal("true")
   }

   @Test
   @Ignore("Needs taxi enhancmenent")
   fun `can parse enums with boolean values`() {
      val schema = TaxiSchema.from(
         """
         enum IsAlive {
            Living(true),
            Dead(false)
         }
      """.trimIndent()
      )
      val enum = PrimitiveParser().parse(true, schema.type("IsAlive"), Provided, format = null)
      enum.type.name.fullyQualifiedName.should.equal("IsAlive")
      // Note -it's a string, because enum values are string by default
      enum.value.should.equal("true")
   }

   @Test
   fun unknownEnumValueFailsParsing() {
      exception.expect(IllegalStateException::class.java)
      exception.expectMessage("Enum Country does not contain either a name nor a value of Great Britain")

      val src = """
enum Country {
   NZ("New Zealand"),
   AUS("Australia")
}
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      TypedInstance.from(schema.type("Country"), "Great Britain", schema)
   }

   @Test
   fun unknownEnumValueMatchesDefault() {
      val src = """
enum Country {
   default NZ("New Zealand"),
   AUS("Australia")
}
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      val enum = TypedInstance.from(schema.type("Country"), "Great Britain", schema) as TypedEnumValue
      enum.type.name.fullyQualifiedName.should.equal("Country")
      enum.name.should.equal("NZ")
   }

   @Test
   fun canParseInheritedEnums() {
      val src = """
enum Country {
   NZ("New Zealand"),
   AUS("Australia")
}
enum CountryCode inherits Country
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      val enum = TypedInstance.from(schema.type("CountryCode"), "NZ", schema) as TypedEnumValue
      enum.type.name.fullyQualifiedName.should.equal("CountryCode")
      enum.name.should.equal("NZ")
   }

   @Test
   fun canParseInheritedPrimitive() {
      val src = """
type OrderNumber inherits String
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      val enum = PrimitiveParser().parse("order_1", schema.type("OrderNumber"), Provided, format = null)
      enum.type.name.fullyQualifiedName.should.equal("OrderNumber")
      enum.value.should.equal("order_1")
   }

   @Test
   fun canParsePrimitive() {
      val src = """
type alias OrderNumber as String
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      val enum = PrimitiveParser().parse("order_1", schema.type("OrderNumber"), Provided, format = null)
      enum.type.name.fullyQualifiedName.should.equal("OrderNumber")
      enum.value.should.equal("order_1")
   }

   // MP 3-Aug: Not throwing this exception anymore, just returning a TypedNull,
   // with details of the parsing failure.
   @Test
   fun primitiveTypeParsingFailure() {
      exception.expect(DataParsingException::class.java)

      val src = """
type alias OrderNumber as Int
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      PrimitiveParser().parse("order_1", schema.type("OrderNumber"), Provided, format = null)
   }

   @Test
   fun `when cannot parse a value a typed null is returned with a meaningful error`() {
      val src = """
type alias OrderNumber as Int
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      val value = PrimitiveParser().parse("order_1", schema.type("OrderNumber"), Provided, parsingErrorBehaviour = ParsingFailureBehaviour.ReturnTypedNull, format = null)
      value.should.be.instanceof(TypedNull::class.java)
      val source = value.source as FailedParsingSource
      source.error.should.equal("""Failed to parse value order_1 to type OrderNumber (no formats were supplied) - Character o is neither a decimal digit number, decimal point, nor "e" notation exponential mark.""")
   }


   @Test
   fun parseLongAsInstant() {
      val src = """
type alias OrderDate as Instant
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      val value = PrimitiveParser().parse(java.lang.Long.valueOf(1575389279798), schema.type("OrderDate"), Provided, format = null)
      value.value.should.equal(Instant.parse("2019-12-03T16:07:59.798Z"))
   }

   @Test
   fun reportMeaningfulException() {
      val src = """
type alias OrderDate as Instant
      """.trimIndent()
      val schema = TaxiSchema.from(src)

      val instance = PrimitiveParser().parse(java.lang.Integer.valueOf(389279798), schema.type("OrderDate"), Provided, ParsingFailureBehaviour.ReturnTypedNull, format = null)
      instance.should.be.instanceof(TypedNull::class.java)
      val source = (instance as TypedNull).source as FailedParsingSource
      source.error.should.equal("""Failed to parse value 389279798 to type OrderDate (no formats were supplied) - Unable to convert value=389279798 to type=class java.time.Instant Error: No converter found capable of converting from type [java.lang.Integer] to type [java.time.Instant]""")

   }
}
