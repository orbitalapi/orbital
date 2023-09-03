package com.orbitalhq

import com.winterbe.expekt.should
import com.orbitalhq.models.ConversionService
import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedValue
import com.orbitalhq.models.conversion.VyneConversionService
import com.orbitalhq.schemas.taxi.TaxiSchema
import lang.taxi.types.PrimitiveType
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class TypedValueTest {

   @Test
   fun `when using core types inside vyne then VyneConversionService is detected`() {
      // Conversion services have moved to a separate jar.
      // we try to detect at runtime if they're available, and if not,
      // fall back to a no-op converter
      ConversionService.DEFAULT_CONVERTER.should.be.instanceof(VyneConversionService::class.java)
   }

   @Test
   fun testStringInstantConversion() {
      val schema = TaxiSchema.from("")
      val instance =
         TypedInstance.from(schema.type(PrimitiveType.INSTANT), "2020-05-14T22:00:00Z", schema, source = Provided)

      instance.value.should.equal(Instant.parse("2020-05-14T22:00:00Z"))
   }

   @Test
   fun canParseDatesWithFormats() {
      val schema = TaxiSchema.from("""
         @Format("dd/MM/yy'T'HH:mm:ss" )
         type KiwiDate inherits Instant
      """.trimIndent())
      val instance = TypedInstance.from(schema.type("KiwiDate"), "28/04/19T22:00:00", schema, source = Provided)
      val instant = instance.value as Instant
      instant.should.equal(Instant.parse("2019-04-28T22:00:00Z"))

   }

   @Test
   fun timeformat() {
      val schema = TaxiSchema.from("""
         type OrderEventTime inherits Time
         model TestTime {
            @Format("HH.mm.s")
            orderTime: OrderEventTime
         }

      """.trimIndent())
      val value = """
         {
            "orderTime": "00.00.6"
         }
      """.trimIndent()
      val instance = TypedInstance.from(schema.type("TestTime"), value, schema, source = Provided)
      val orderTime = instance.value as Map<String, TypedValue>
      val time = orderTime["orderTime"]?.value as LocalTime
      time.should.equal(LocalTime.of(0, 0, 6))

   }

   @Test
   fun canParseNumbersWithCommas() {
      val schema = TaxiSchema.from("")
      fun toType(value:Any, type:PrimitiveType):Any {
         return TypedInstance.from(schema.type(type.qualifiedName), value, schema, source = Provided).value!!
      }

      toType("6,300.00",PrimitiveType.INTEGER).should.equal(6300)
      toType("6,300.00",PrimitiveType.DECIMAL).should.satisfy { (it as BigDecimal).compareTo(BigDecimal("6300.00")) == 0 }
      // Need to bump taxi version
//      toType("6,300.00",PrimitiveType.DOUBLE).should.equal(6300.0)

   }
   @Test
   fun shouldParseIntsWithTrailingZerosAsInts() {
      val schema = TaxiSchema.from("")
      val instance = TypedInstance.from(schema.type(PrimitiveType.INTEGER), "10.00", schema, source = Provided)

      instance.value.should.equal(10)
   }

   @Test
   fun `should handle decimals in scientific format`() {
      val schema = TaxiSchema.from("")
      val instance = TypedInstance.from(schema.type(PrimitiveType.DECIMAL), "2.50E+07", schema, source = Provided)
      instance.value.should.equal(BigDecimal("2.50E+07"))
   }

   @Test
   fun `should handle ints in scientific format`() {
      val schema = TaxiSchema.from("")
      val instance = TypedInstance.from(schema.type(PrimitiveType.INTEGER), "2.50E+02", schema, source = Provided)
      instance.value.should.equal(250)
   }

   @Test
   fun `should handle doubles in scientific format`() {
      val schema = TaxiSchema.from("")
      val instance = TypedInstance.from(schema.type(PrimitiveType.DOUBLE), "2.512E+03", schema, source = Provided)
      instance.value.should.equal(2512.0)
   }

   @Test
   fun canParseDateOnly() {
      val schema = TaxiSchema.from("""
         @Format("yyyy-MM-dd")
         @Format("dd/MM/yyyy HH:mm")
         type MaturityDateDate inherits Date
      """.trimIndent())
      val instance = TypedInstance.from(schema.type("MaturityDateDate"), "09/04/2025 00:00", schema, source = Provided)
      val date = instance.value as LocalDate
      date.should.equal(LocalDate.of(2025, 4, 9))

   }

   @Test
   fun `Can Handle Instants with microsecond resolution`() {
      val schema = TaxiSchema.from("""
         @Format( "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'")
         type KiwiDate inherits Instant
      """.trimIndent())

      val instance = TypedInstance.from(schema.type("KiwiDate"), 1608034621.123456, schema, source = Provided)
      val instant = instance.value as Instant
      instant.should.equal(Instant.parse("2020-12-15T12:17:01.123456Z"))
   }

   @Test
   fun `Can Handle Instants with microsecond resolution for DateTime types`() {
      val schema = TaxiSchema.from("""
         @Format("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'")
         type KiwiDate inherits DateTime
      """.trimIndent())

      val instance = TypedInstance.from(schema.type("KiwiDate"), 1608034621.123456, schema, source = Provided)
      val localDateTime = instance.value as LocalDateTime
      localDateTime.should.equal(LocalDateTime.parse("2020-12-15T12:17:01.123456"))
   }
}