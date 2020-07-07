package io.vyne

import com.winterbe.expekt.should
import io.vyne.models.DataParsingException
import io.vyne.models.PrimitiveParser
import io.vyne.models.Provided
import io.vyne.schemas.taxi.TaxiSchema
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
      val enum = PrimitiveParser().parse("NZ",schema.type("Country"), Provided)
      enum.type.name.fullyQualifiedName.should.equal("Country")
      enum.value.should.equal("NZ")
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
      val enum = PrimitiveParser().parse("New Zealand",schema.type("Country"), Provided)
      enum.type.name.fullyQualifiedName.should.equal("Country")
      enum.value.should.equal("New Zealand")
   }

   @Test
   fun unknownEnumValueFailsParsing() {
      exception.expect(IllegalStateException::class.java)
      exception.expectMessage("""Unable to map Value=Great Britain to Enum Type=Country, allowed values=[(NZ, New Zealand), (AUS, Australia)""")

      val src = """
enum Country {
   NZ("New Zealand"),
   AUS("Australia")
}
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      PrimitiveParser().parse("Great Britain",schema.type("Country"), Provided)
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
      val enum = PrimitiveParser().parse("NZ",schema.type("CountryCode"), Provided)
      enum.type.name.fullyQualifiedName.should.equal("CountryCode")
      enum.value.should.equal("NZ")
   }

   @Test
   fun canParseInheritedPrimitive() {
      val src = """
type OrderNumber inherits String
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      val enum = PrimitiveParser().parse("order_1",schema.type("OrderNumber"), Provided)
      enum.type.name.fullyQualifiedName.should.equal("OrderNumber")
      enum.value.should.equal("order_1")
   }

   @Test
   fun canParsePrimitive() {
      val src = """
type alias OrderNumber as String
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      val enum = PrimitiveParser().parse("order_1",schema.type("OrderNumber"), Provided)
      enum.type.name.fullyQualifiedName.should.equal("OrderNumber")
      enum.value.should.equal("order_1")
   }

   @Test
   fun primitiveTypeParsingFailure() {
      exception.expect(DataParsingException::class.java)

      val src = """
type alias OrderNumber as Int
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      PrimitiveParser().parse("order_1",schema.type("OrderNumber"), Provided)
   }

   @Test
   fun parseLongAsInstant() {
      val src = """
type alias OrderDate as Instant
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      val value = PrimitiveParser().parse(java.lang.Long.valueOf(1575389279798), schema.type("OrderDate"), Provided)
      value.value.should.equal(Instant.parse("2019-12-03T16:07:59.798Z"))
   }

   @Test
   fun reportMeaningfulException() {
      exception.expect(DataParsingException::class.java)
      exception.expectMessage("""Unable to convert value=389279798 to type=class java.time.Instant Error: No converter found capable of converting from type [java.lang.Integer] to type [java.time.Instant]""")

      val src = """
type alias OrderDate as Instant
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      PrimitiveParser().parse(java.lang.Integer.valueOf(389279798), schema.type("OrderDate"), Provided)

   }
}
