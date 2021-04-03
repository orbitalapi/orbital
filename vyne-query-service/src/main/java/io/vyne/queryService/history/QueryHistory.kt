package io.vyne.queryService.history

import com.google.common.collect.EvictingQueue
import io.vyne.query.history.QueryHistoryRecord
import io.vyne.utils.log
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface QueryHistory {
   fun add(recordProvider: () -> QueryHistoryRecord<out Any>)
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
   override fun add(recordProvider: () -> QueryHistoryRecord<out Any>) {
      val record = recordProvider()
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




