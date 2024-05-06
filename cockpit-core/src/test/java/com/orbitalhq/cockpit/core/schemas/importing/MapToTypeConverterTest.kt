package com.orbitalhq.cockpit.core.schemas.importing

import com.orbitalhq.cockpit.core.schemas.importing.map.MapToTypeBuilder
import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import lang.taxi.types.ArrayType
import lang.taxi.types.Field
import lang.taxi.types.PrimitiveType
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class MapToTypeConverterTest {
   @Test
   fun buildsGenericType() {
      val source = mapOf(
         "groups" to listOf("Admin","Users"),
         "name"  to "Jimmy",
         "age" to 43,
         "ageInSeconds" to 2460124601L,
         "ageInDecimal" to BigDecimal.valueOf(24601),
         "currentTime" to Instant.parse("2024-04-23T17:40:00Z")
      )
      val converter = MapToTypeBuilder(TaxiSchema.empty())
      val type = converter.convert(source, "Person")
      type.fields.shouldHaveSize(6)
      type.field("name").shouldHaveType("Name", PrimitiveType.STRING)
      type.field("age").shouldHaveType("Age", PrimitiveType.INTEGER)
      type.field("currentTime").shouldHaveType("CurrentTime", PrimitiveType.INSTANT)
      type.field("groups").type.shouldBeInstanceOf<ArrayType>()
         .memberType.basePrimitive.shouldBe(PrimitiveType.STRING)
   }

}

fun Field.shouldHaveType(name: String, baseType: PrimitiveType):Field {
   this.type.inheritsFrom.shouldContain(baseType)
   this.type.qualifiedName.shouldBe(name)
   return this
}
