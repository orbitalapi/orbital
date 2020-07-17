package io.vyne.queryService
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.google.common.collect.EvictingQueue
import io.vyne.query.HistoryQueryResponse
import io.vyne.query.Query
import io.vyne.models.DataSource
import io.vyne.models.TypeNamedInstance
import io.vyne.query.*
import io.vyne.schemas.Path
import io.vyne.schemas.QualifiedName
import io.vyne.utils.log
import io.vyne.vyneql.VyneQLQueryString
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

interface QueryHistory {
   fun add(record: QueryHistoryRecord<out Any>)
   fun list(): Flux<QueryHistoryRecord<out Any>>
   fun get(id: String): Mono<QueryHistoryRecord<out Any>>
}
@Component
@ConditionalOnExpression("T(org.springframework.util.StringUtils).isEmpty('\${spring.r2dbc.url:}') and \${vyne.query-history.enabled:true}")
class InMemoryQueryHistory : QueryHistory {
   private val queries = EvictingQueue.create<QueryHistoryRecord<out Any>>(10);
   override fun add(record: QueryHistoryRecord<out Any>) {
      this.queries.add(record);
      log().info("Saving to query history /query/history/${record.id}/profile")
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
}

data class VyneQlQueryHistoryRecord(
   override val query: VyneQLQueryString,
   override val response: HistoryQueryResponse,
   override val timestamp: Instant = Instant.now()
) : QueryHistoryRecord<VyneQLQueryString>
data class RestfulQueryHistoryRecord(
   override val query: Query,
   override val response: HistoryQueryResponse,
   override val timestamp: Instant = Instant.now()
) : QueryHistoryRecord<Query>


