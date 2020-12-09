package io.vyne.queryService

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.google.common.collect.EvictingQueue
import io.vyne.query.HistoryQueryResponse
import io.vyne.query.Query
import io.vyne.utils.log
import io.vyne.vyneql.VyneQLQueryString
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

interface QueryHistory {
   fun add(record: QueryHistoryRecord<out Any>)
   fun list(): Flux<QueryHistoryRecord<out Any>>
   fun get(id: String): Mono<QueryHistoryRecord<out Any>>
   fun clear()
}

@Component
@ConditionalOnExpression("T(org.springframework.util.StringUtils).isEmpty('\${spring.r2dbc.url:}') and \${vyne.query-history.enabled:true}")
class InMemoryQueryHistory(
   @Value("\${vyne.query-history.in-memory-limit:10}") val historySize: Int = 10
) : QueryHistory {
   private val queries = EvictingQueue.create<QueryHistoryRecord<out Any>>(historySize)
   override fun add(record: QueryHistoryRecord<out Any>) {
      this.queries.add(record);
      log().info("Saving to query history /query/history/${record.id}/profile")
   }

   override fun clear() {
      this.queries.clear()
   }

   override fun list(): Flux<QueryHistoryRecord<out Any>> {
      return Flux.fromArray(
         this.queries
            .toList()
            .reversed()
            .toTypedArray())
   }

   override fun get(id: String): Mono<QueryHistoryRecord<out Any>> {
      // There's only 50 at the moment, so indexing isn't worth it.
      // Address this later.
      val match = queries.first { it.id == id }
      return Mono.just(match)
   }
}

@JsonTypeInfo(
   use = JsonTypeInfo.Id.CLASS,
   include = JsonTypeInfo.As.PROPERTY,
   property = "className")
interface QueryHistoryRecord<T> {
   val query: T
   val response: HistoryQueryResponse
   val timestamp: Instant
   val id: String
      get() {
         return response.queryResponseId
      }

   fun withResponse(response: HistoryQueryResponse): QueryHistoryRecord<T>
}

data class VyneQlQueryHistoryRecord(
   override val query: VyneQLQueryString,
   override val response: HistoryQueryResponse,
   override val timestamp: Instant = Instant.now()
) : QueryHistoryRecord<VyneQLQueryString> {
   override fun withResponse(historyQueryResponse: HistoryQueryResponse): QueryHistoryRecord<VyneQLQueryString> {
      return copy(response = historyQueryResponse)
   }
}

data class RestfulQueryHistoryRecord(
   override val query: Query,
   override val response: HistoryQueryResponse,
   override val timestamp: Instant = Instant.now()
) : QueryHistoryRecord<Query> {
   override fun withResponse(historyQueryResponse: HistoryQueryResponse): QueryHistoryRecord<Query> {
      return copy(response = historyQueryResponse)
   }
}


