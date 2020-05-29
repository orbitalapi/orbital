package io.vyne

import com.winterbe.expekt.should
import io.vyne.models.PrimitiveParser
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import java.lang.IllegalStateException


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
      val enum = PrimitiveParser().parse("NZ",schema.type("Country"), schema)
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
      val enum = PrimitiveParser().parse("New Zealand",schema.type("Country"), schema)
      enum.type.name.fullyQualifiedName.should.equal("Country")
      enum.value.should.equal("NZ")
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
      PrimitiveParser().parse("Great Britain",schema.type("Country"), schema)
   }

   @Test
   fun canParseInheritedEnums() {
      val src = """
enum Country {
   NZ("New Zealand"),
   AUS("Australia")
}
type CountryCode inherits Country
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      val enum = PrimitiveParser().parse("NZ",schema.type("CountryCode"), schema)
      enum.type.name.fullyQualifiedName.should.equal("Country")
      enum.value.should.equal("NZ")
   }

   @Test
   fun canParsePrimitive() {
      val src = """
type alias OrderNumber as String
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      val enum = PrimitiveParser().parse("order_1",schema.type("OrderNumber"), schema)
      enum.type.name.fullyQualifiedName.should.equal("OrderNumber")
      enum.value.should.equal("order_1")
   }

   @Test
   fun primitiveTypeParsingFailure() {
      exception.expect(IllegalArgumentException::class.java)
      exception.expectMessage("""Cannot deserialize value of type `int` from String "order_1": not a valid Integer value
 at [Source: UNKNOWN; line: -1, column: -1]""")

      val src = """
type alias OrderNumber as Int
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      PrimitiveParser().parse("order_1",schema.type("OrderNumber"), schema)
   }
}
