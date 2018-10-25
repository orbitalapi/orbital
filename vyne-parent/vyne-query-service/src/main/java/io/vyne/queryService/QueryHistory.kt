package io.vyne.queryService

import com.google.common.collect.EvictingQueue
import io.vyne.query.Query
import io.vyne.query.QueryResponse
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class QueryHistory {
   private val queries = EvictingQueue.create<QueryHistoryRecord>(50);

   fun add(record: QueryHistoryRecord) {
      this.queries.add(record);
   }

   fun list(): List<QueryHistoryRecord> {
      return this.queries.toList()
   }
}

data class QueryHistoryRecord(
   val query: Query,
   val response: QueryResponse,
   val timestamp: Instant = Instant.now()
)
