package io.vyne

import io.vyne.query.Fact
import io.vyne.schemas.QueryOptions
import io.vyne.schemas.Schema

interface VyneProvider {
   fun createVyne(facts: Set<Fact> = emptySet()): Vyne
   fun createVyne(
      facts: Set<Fact> = emptySet(),
      schema: Schema,
      queryOptions: QueryOptions = QueryOptions.default()
   ): Vyne

}
