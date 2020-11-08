package io.vyne.queryService

import io.vyne.models.DataSource
import io.vyne.query.ProfilerOperationDTO
import io.vyne.query.QueryResponse
import io.vyne.schemas.QualifiedName
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.HttpClientErrorException
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
   fun getHistoryRecord(@PathVariable("id") queryId: String): Mono<QueryHistoryRecord<out Any>> {
      return this.history.get(queryId)

   }

   /**
    * Returns a node detail, containing the actual type name, and lineage
    * for a specific value within a query history.
    * A nodeId takes the format of `[0].orderId`
    * The format is as follows:
    *  * JsonPath-like syntax, without the $. prefix
    *  * For array index access, use `[n]`
    *  * Otherwise, use the property name
    */
   @GetMapping("/api/query/history/{id}/{queryType}/{nodeId}")
   fun getNodeDetail(@PathVariable("id") queryId: String,
                     @PathVariable("queryType") queryType: String,
                     @PathVariable("nodeId") nodeId: String): Mono<QueryResultNodeDetail> {
      return history.get(queryId).map { historyRecord ->
         val queryTypeResults = historyRecord.response.results[queryType] ?: throw HttpClientErrorException(
            HttpStatus.BAD_REQUEST,"Type $queryType is not present within query result $queryId"
         )
         val nodeParts = nodeId.split(".")
         QueryHistoryResultNodeFinder.find(nodeParts,queryTypeResults, nodeId)
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

data class QueryResultNodeDetail(
   val attributeName:String,
   val path:String,
   val typeName: QualifiedName,
   val source: DataSource?
)

data class QueryHistoryRecordSummary<T>(
   val queryId: String,
   val query: T,
   val responseStatus: QueryResponse.ResponseStatus,
   val durationMs: Long,
   val recordSize: Int,
   val timestamp: Instant
)
