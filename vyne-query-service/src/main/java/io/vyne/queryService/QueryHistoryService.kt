package io.vyne.queryService

import io.vyne.query.Lineage
import io.vyne.query.ProfilerOperationDTO
import io.vyne.query.QueryResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.time.Instant
import kotlin.streams.toList

@RestController
class QueryHistoryService(private val history: QueryHistory, private val queryHistoryExporter: QueryHistoryExporter) {
   private val truncationThreshold = 10

   @GetMapping("/api/query/history")
   fun listHistory(): List<QueryHistoryRecordSummary<Any>> {

      return history.list()
         .map { historyRecord ->
            QueryHistoryRecordSummary(
               historyRecord.id,
               historyRecord.query,
               historyRecord.response.responseStatus,
               historyRecord.response.durationMs,
               historyRecord.response.resultSize,
               historyRecord.timestamp
            )
         }
         .toStream().toList()
   }

   @GetMapping("/api/query/history/{id}")
   fun getHistoryRecord(@PathVariable("id") queryId: String): Mono<String> {
      return this.history.get(queryId)
         .map { record ->
            Lineage.newLineageAwareJsonMapper()
               .writeValueAsString(record)
         }
   }

   @GetMapping("/api/query/history/{id}/profile")
   fun getQueryProfile(@PathVariable("id") queryId: String): Mono<ProfilerOperationDTO?> {
      return history.get(queryId).map { it.response.profilerOperation }
   }

   @GetMapping("/api/query/history/{id}/{type}/export")
   fun getQueryExport(@PathVariable("id") queryId: String, @PathVariable("type") exportType: ExportType): Mono<ByteArray> {
      return history.get(queryId).map {
         queryHistoryExporter.export(it.response.results, exportType)
      }
   }
}

data class QueryHistoryRecordSummary<T>(
   val queryId: String,
   val query: T,
   val responseStatus: QueryResponse.ResponseStatus,
   val durationMs: Long,
   val recordSize: Int,
   val timestamp: Instant
)
