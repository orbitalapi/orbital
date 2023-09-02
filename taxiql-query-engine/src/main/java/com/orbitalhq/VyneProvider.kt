package com.orbitalhq

import com.orbitalhq.query.Fact
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.Schema

interface VyneProvider {
   fun createVyne(facts: Set<Fact> = emptySet()): Vyne
   fun createVyne(
      facts: Set<Fact> = emptySet(),
      schema: Schema,
      queryOptions: QueryOptions = QueryOptions.default()
   ): Vyne

}
