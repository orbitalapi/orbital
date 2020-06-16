package io.vyne.queryService

import com.google.common.collect.EvictingQueue
import io.vyne.query.Query
import io.vyne.query.QueryResponse
import io.vyne.vyneql.VyneQLQueryString
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class QueryHistory {
   private val queries = EvictingQueue.create<QueryHistoryRecord<out Any>>(20);

   fun add(record: QueryHistoryRecord<out Any>) {
      this.queries.add(record);
   }

   fun list(): List<QueryHistoryRecord<out Any>> {
      return this.queries.toList()
         .reversed()
   }

   fun get(id: String): QueryHistoryRecord<out Any> {
      // There's only 50 at the moment, so indexing isn't worth it.
      // Address this later.
      return queries.first { it.id == id }
   }
}

interface QueryHistoryRecord<T> {
   val query: T
   val response: QueryResponse
   val timestamp: Instant
   val id: String
      get() {
         return response.queryResponseId
      }
}

data class VyneQlQueryHistoryRecord(
   override val query: VyneQLQueryString,
   override val response: QueryResponse,
   override val timestamp: Instant = Instant.now()
) : QueryHistoryRecord<VyneQLQueryString>

data class RestfulQueryHistoryRecord(
   override val query: Query,
   override val response: QueryResponse,
   override val timestamp: Instant = Instant.now()
) : QueryHistoryRecord<Query>
