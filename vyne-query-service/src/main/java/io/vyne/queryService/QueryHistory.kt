package io.vyne.queryService

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.google.common.collect.EvictingQueue
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
import java.util.*

interface QueryHistory {
   fun add(record: QueryHistoryRecord<out Any>)
   fun list(): Flux<QueryHistoryRecord<out Any>>
   fun get(id: String): Mono<QueryHistoryRecord<out Any>>
}

@Component
@ConditionalOnExpression("T(org.springframework.util.StringUtils).isEmpty('\${spring.r2dbc.url:}') and \${app.query-history.enabled:true}")
class InMemoryQueryHistory: QueryHistory {

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

data class HistoryQueryResponse(val results: Map<String, Any?>,
                                val resultsVerbose: Map<String, List<TypeNamedInstance>>,
                                val sources: List<DataSource>,
                                val unmatchedNodes: List<QualifiedName>,
                                val path: Path? = null,
                                val queryResponseId: String = UUID.randomUUID().toString(),
                                val resultMode: ResultMode,
                                val profilerOperation: ProfilerOperationDTO?,
                                val remoteCalls: List<RemoteCall>,
                                val timings: Map<OperationType, Long>,
                                @get:JsonProperty("fullyResolved")
                                val fullyResolved: Boolean,
                                val truncated: Boolean? = false) {
   companion object {
      fun from(response: QueryResponse): HistoryQueryResponse {
         return when (response) {
            is QueryResult -> {
               val queryResultGraph = QueryResultGraph(response.results, response.resultMode)
               return HistoryQueryResponse(
                  response.resultMap,
                  queryResultGraph.buildResultsVerbose(),
                  queryResultGraph.resultSources,
                  response.unmatchedNodeNames,
                  response.path,
                  response.queryResponseId,
                  response.resultMode,
                  response.profilerOperation?.toDto(),
                  response.remoteCalls,
                  response.timings,
                  response.isFullyResolved)
            }
            else -> {
               HistoryQueryResponse(
                  emptyMap(),
                  emptyMap(),
                  listOf(),
                  emptyList(),
                  null,
                  response.queryResponseId,
                  response.resultMode,
                  response.profilerOperation?.toDto(),
                  emptyList(),
                  emptyMap(),
                  false)
            }
         }
      }
   }
}

