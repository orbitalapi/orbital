package io.vyne.queryService.history

import io.vyne.models.DataSource
import io.vyne.query.ProfilerOperationDTO
import io.vyne.query.QueryResponse
import io.vyne.queryService.history.db.PersistentQuerySummary
import io.vyne.queryService.history.db.QueryHistoryRecordRepository
import io.vyne.schemas.QualifiedName
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

@RestController
class QueryHistoryService(
   private val history: QueryHistoryRecordRepository,
   private val queryHistoryExporter: QueryHistoryExporter,
//   private val regressionPackProvider: RegressionPackProvider
) {

   @DeleteMapping("/api/query/history")
   fun clearHistory() {
      history.deleteAll().subscribe()
   }

   @GetMapping("/api/query/history")
   fun listHistory(): Flux<PersistentQuerySummary> {
      return history.findAll()
   }

   @GetMapping("/api/query/history/{id}")
   fun getHistoryRecord(@PathVariable("id") queryId: String): Mono<PersistentQuerySummary> {
      return this.history.findByQueryId(queryId)

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

   /*
   @GetMapping("/api/query/history/{id}/{queryType}/{nodeId}")
   @JsonView(DataSourceIncludedView::class)
   fun getNodeDetail(@PathVariable("id") queryId: String,
                     @PathVariable("queryType") queryType: String,
                     @PathVariable("nodeId") nodeId: String): Mono<QueryResultNodeDetail> {
      return history.get(queryId).map { historyRecord ->
         val queryTypeResults = historyRecord.response?.results?[queryType] ?: throw HttpClientErrorException(
            HttpStatus.BAD_REQUEST,"Type $queryType is not present within query result $queryId"
         )
         val nodeParts = nodeId.split(".")
         QueryHistoryResultNodeFinder.find(nodeParts,queryTypeResults, nodeId)
      }
   }*/

   @GetMapping("/api/query/history/{id}/profile")
   fun getQueryProfile(@PathVariable("id") queryId: String): Mono<ProfilerOperationDTO?> {
//      return history.get(queryId).map { it.response.profilerOperation }
      TODO()
   }

   @GetMapping("/api/query/history/{id}/{type}/export")
   fun getQueryExport(
      @PathVariable("id") queryId: String,
      @PathVariable("type") exportType: ExportType
   ): Mono<ByteArray> {
      TODO()
//      return history.get(queryId).map {
//         queryHistoryExporter.export(it.response.results!!, exportType)
//      }
   }

   @PostMapping("/api/query/history/{id}/regressionPack")
   fun getRegressionPack(@RequestBody request: RegressionPackRequest): Mono<StreamingResponseBody> {
//      return regressionPackProvider.createRegressionPack(request)
      TODO()
   }

}

data class RegressionPackRequest(val queryId: String, val regressionPackName: String)

data class QueryResultNodeDetail(
   val attributeName: String,
   val path: String,
   val typeName: QualifiedName,
   val source: DataSource?
)

data class QueryHistoryRecordSummary<T>(
   val queryId: String,
   val query: T,
   val responseStatus: QueryResponse.ResponseStatus,
   val durationMs: Long,
   val recordSize: Int?,
   val timestamp: Instant
)
