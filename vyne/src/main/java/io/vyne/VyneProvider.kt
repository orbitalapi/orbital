package io.vyne

import io.vyne.query.Fact
import io.vyne.schemas.Schema

interface VyneProvider {
   fun createVyne(facts: Set<Fact> = emptySet()): Vyne
   fun createVyne(facts: Set<Fact> = emptySet(), schema: Schema): Vyne
}
