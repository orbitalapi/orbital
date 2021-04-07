package io.vyne.queryService.history

import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.queryService.FirstEntryMetadataResultSerializer
import io.vyne.queryService.history.db.LineageRecordRepository
import io.vyne.queryService.history.db.PersistentQuerySummary
import io.vyne.queryService.history.db.QueryHistoryRecordRepository
import io.vyne.queryService.history.db.QueryResultRowRepository
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.fqn
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class QueryHistoryService(
   private val history: QueryHistoryRecordRepository,
   private val queryResultRowRepository: QueryResultRowRepository,
   private val lineageRecordRepository: LineageRecordRepository,
   private val queryHistoryExporter: QueryHistoryExporter,
   private val mapper: ObjectMapper
//   private val regressionPackProvider: RegressionPackProvider
) {

   @DeleteMapping("/api/query/history")
   fun clearHistory() {
      history.deleteAll().subscribe()
   }

   @GetMapping("/api/query/history")
   fun listHistory(): Flux<PersistentQuerySummary> {
      return history.findAllByOrderByStartTimeDesc()
         .take(50)

   }

   /**
    * Returns the results (as JSON of TypeNamedInstances) over server-sent-events
    */
   @GetMapping(
      "/api/query/history/{id}/results", produces = [
         MediaType.TEXT_EVENT_STREAM_VALUE,
         MediaType.APPLICATION_JSON_VALUE,
      ]
   )
   fun getHistoryRecordStream(
      @PathVariable("id") queryId: String,
      @RequestParam("limit", required = false) limit: Long? = null
   ): Flux<FirstEntryMetadataResultSerializer.ValueWithTypeName> {
      val flux = if (limit != null) {
         this.queryResultRowRepository.findAllByQueryId(queryId)
            .take(limit)
      } else {
         this.queryResultRowRepository.findAllByQueryId(queryId)
      }
      return flux.mapNotNull { resultRow ->
         val typeNamedInstance = resultRow.asTypeNamedInstance(mapper)
         FirstEntryMetadataResultSerializer.ValueWithTypeName(
            typeNamedInstance.typeName.fqn(),
            emptySet(),
            typeNamedInstance.convertToRaw()!!,
            resultRow.valueHash
         )
      }

   }


   @GetMapping("/api/query/history/{id}/dataSource/{rowId}/{attributePath}")
   fun getNodeDetail(
      @PathVariable("id") queryId: String,
      @PathVariable("rowId") rowValueHash: Int,
      @PathVariable("attributePath") attributePath: String
   ): Mono<QueryResultNodeDetail> {
      return queryResultRowRepository.findByQueryIdAndValueHash(queryId, rowValueHash)
         .next()
         .flatMap { resultRow ->
            val typeNamedInstance = resultRow.asTypeNamedInstance(mapper)
            val nodeParts = attributePath.split(".")
            val nodeDetail = QueryHistoryResultNodeFinder.find(nodeParts, typeNamedInstance, attributePath)
            if (nodeDetail.dataSourceId == null) {
               error("No dataSourceId is present on TypeNamedInstance for query $queryId row $rowValueHash path $attributePath")
            }
            lineageRecordRepository.findById(nodeDetail.dataSourceId)
               .map { lineageRecord -> nodeDetail.copy(source = lineageRecord.dataSourceJson) }
         }
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
   val dataSourceId: String?,
   @JsonRawValue
   val source: String? // json of the DataSource
)

