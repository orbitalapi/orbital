package com.orbitalhq

import com.orbitalhq.query.EmitMetrics
import com.orbitalhq.query.Fact
import com.orbitalhq.query.QueryResult
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.Schema

interface VyneProvider {
   fun createVyne(facts: Set<Fact> = emptySet()): Vyne
   fun createVyne(
      facts: Set<Fact> = emptySet(),
      schema: Schema,
      queryOptions: QueryOptions = QueryOptions.default()
   ): Vyne

   // MP: 1-May-25: This is part of a suboptimal solution,
// let's get rid of it asap
// Need a way of monitoring the error stream from the
// QueryResult that only monitors the "outer" query stream
   fun emitMetrics(queryResult: QueryResult, emitMetrics: EmitMetrics) {}

}
