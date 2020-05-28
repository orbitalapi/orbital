package io.vyne

import com.winterbe.expekt.should
import io.vyne.models.PrimitiveParser
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test

// Note - this tests a class in vyne-core-types.  Testing there is awkward because of lack of access to taxi-schema
class PrimitiveParserTest {
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
   }
}
