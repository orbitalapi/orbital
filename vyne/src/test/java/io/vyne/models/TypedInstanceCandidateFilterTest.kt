package io.vyne.models

import com.winterbe.expekt.should
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test
import org.springframework.format.annotation.DateTimeFormat

class TypedInstanceCandidateFilterTest {
   val schema = TaxiSchema.from("""
        type InstrumentId inherits String
        type UnderlyingInstrumentId inherits InstrumentId
        type StrategyInstrumentId inherits InstrumentId
      """.trimIndent())

   @Test
   fun `returns TypedNull when alternatives are all typed null`() {
      val instrumentIdType = schema.type("InstrumentId")
      val underlyingInstrumentId = schema.type("UnderlyingInstrumentId")
      val strategyInstrumentId = schema.type("StrategyInstrumentId")
      TypedInstanceCandidateFilter.resolve(listOf(TypedNull(strategyInstrumentId), TypedNull(underlyingInstrumentId)), instrumentIdType).value.should.be.`null`
   }
}
