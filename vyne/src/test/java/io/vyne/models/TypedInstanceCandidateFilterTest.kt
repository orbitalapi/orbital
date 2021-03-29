package io.vyne.models

import com.winterbe.expekt.should
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test

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
      TypedInstanceCandidateFilter.resolve(listOf(TypedNull.create(strategyInstrumentId), TypedNull.create(underlyingInstrumentId)), instrumentIdType).value.should.be.`null`
   }
}
