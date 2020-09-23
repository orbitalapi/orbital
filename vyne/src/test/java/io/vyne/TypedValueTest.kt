package io.vyne

import com.winterbe.expekt.should
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.schemas.Modifier
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.types.PrimitiveType
import org.junit.Test
import org.mockito.Mockito.mock
import java.math.BigDecimal
import java.time.Instant

class TypedValueTest {
   @Test
   fun testStringInstantConversion() {
      val schema = TaxiSchema.from("")
      val instance = TypedInstance.from(schema.type(PrimitiveType.INSTANT), "2020-05-14T22:00:00Z", schema, source = Provided)

      instance.value.should.equal(Instant.parse("2020-05-14T22:00:00Z"))
   }

   @Test
   fun canParseDatesWithFormats() {
      val schema = TaxiSchema.from("""
         type KiwiDate inherits Instant( @format = "dd/MM/yy'T'HH:mm:ss" )
      """.trimIndent())
      val instance = TypedInstance.from(schema.type("KiwiDate"), "28/04/19T22:00:00", schema, source = Provided)
      val instant = instance.value as Instant
      instant.should.equal(Instant.parse("2019-04-28T22:00:00Z"))

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
}
