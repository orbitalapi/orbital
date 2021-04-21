package io.vyne.queryService.history

import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.vyne.models.OperationResult
import io.vyne.query.QueryProfileData
import io.vyne.query.history.QuerySummary
import io.vyne.queryService.FirstEntryMetadataResultSerializer
import io.vyne.queryService.NotFoundException
import io.vyne.queryService.RegressionPackProvider
import io.vyne.queryService.history.db.LineageRecordRepository
import io.vyne.queryService.history.db.QueryHistoryRecordRepository
import io.vyne.queryService.history.db.QueryResultRowRepository
import io.vyne.schemaStore.SchemaSourceProvider
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.fqn
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactor.asFlux
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


@FlowPreview
@RestController
class QueryHistoryService(
   private val queryHistoryRecordRepository: QueryHistoryRecordRepository,
   private val queryResultRowRepository: QueryResultRowRepository,
   private val lineageRecordRepository: LineageRecordRepository,
   private val queryHistoryExporter: QueryHistoryExporter,
   private val objectMapper: ObjectMapper,
   private val regressionPackProvider: RegressionPackProvider
) {

   @DeleteMapping("/api/query/history")
   fun clearHistory() {
      queryHistoryRecordRepository.deleteAll().subscribe()
   }

   @GetMapping("/api/query/history")
   fun listHistory(): Flux<QuerySummary> {
      return queryHistoryRecordRepository.findAllByOrderByStartTimeDesc()
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
      return queryHistoryRecordRepository.findByQueryId(queryId).flatMapMany { querySummary ->
         val flux = if (limit != null) {
            this.queryResultRowRepository.findAllByQueryId(queryId)
               .take(limit)
         } else {
            this.queryResultRowRepository.findAllByQueryId(queryId)
         }
         var firstRowEmitted = false
         flux.mapNotNull { resultRow ->
            val typeNamedInstance = resultRow.asTypeNamedInstance(objectMapper)
            // Only include anonymous type data on the first emitted entry, as they're
            // really expensive to send
            val anonymousTypes = when {
               firstRowEmitted -> FirstEntryMetadataResultSerializer.ValueWithTypeName.NO_ANONYMOUS_TYPES
               !firstRowEmitted -> {
                  firstRowEmitted = true
                  querySummary.anonymousTypesJson
                     ?: FirstEntryMetadataResultSerializer.ValueWithTypeName.NO_ANONYMOUS_TYPES
               }
               else -> error("This shouldn't happen")
            }
            val record = FirstEntryMetadataResultSerializer.ValueWithTypeName(
               typeNamedInstance.typeName.fqn(),
               anonymousTypes,
               typeNamedInstance.convertToRaw()!!,
               resultRow.valueHash,
               resultRow.queryId
            )
            firstRowEmitted = true
            record
         }
      }


   }

   @GetMapping("/api/query/history/clientId/{id}/dataSource/{rowId}/{attributePath}")
   fun getNodeDetailFromClientQueryId(
      @PathVariable("id") clientQueryId: String,
      @PathVariable("rowId") rowValueHash: Int,
      @PathVariable("attributePath") attributePath: String
   ): Mono<QueryResultNodeDetail> {
      return queryHistoryRecordRepository.findByClientQueryId(clientQueryId)
         .flatMap { querySummary ->
            getNodeDetail(querySummary.queryId, rowValueHash, attributePath)
         }
   }

   @GetMapping("/api/query/history/{id}/dataSource/{rowId}/{attributePath}")
   fun getNodeDetail(
      @PathVariable("id") queryId: String,
      @PathVariable("rowId") rowValueHash: Int,
      @PathVariable("attributePath") attributePath: String
   ): Mono<QueryResultNodeDetail> {
      return queryResultRowRepository.findByQueryIdAndValueHash(queryId, rowValueHash)
         .switchIfEmpty(Mono.defer { throw NotFoundException("No query result found with queryId $queryId and row $rowValueHash") })
         .next()
         .flatMap { resultRow ->
            val typeNamedInstance = resultRow.asTypeNamedInstance(objectMapper)
            val nodeParts = attributePath.split(".")
            val nodeDetail = QueryHistoryResultNodeFinder.find(nodeParts, typeNamedInstance, attributePath)
            if (nodeDetail.dataSourceId == null) {
               error("No dataSourceId is present on TypeNamedInstance for query $queryId row $rowValueHash path $attributePath")
            }
            lineageRecordRepository.findById(nodeDetail.dataSourceId)
               .map { lineageRecord -> nodeDetail.copy(source = lineageRecord.dataSourceJson) }
         }
   }

   @GetMapping("/api/query/history/{id}/{format}/export")
   fun exportQueryResults(
      @PathVariable("id") queryId: String,
      @PathVariable("format") exportFormat: ExportFormat,
      serverResponse: ServerHttpResponse
   ): Mono<Void> {
      val bufferFactory = serverResponse.bufferFactory()
      val dataBuffers = queryHistoryExporter.export(queryId, exportFormat)
         .asFlux()
         .map { bufferFactory.wrap(it.toString().toByteArray()) }
      return serverResponse.writeWith(dataBuffers)
   }

   @GetMapping("/api/query/history/clientId/{id}/{format}/export")
   fun exportQueryResultsFromClientId(
      @PathVariable("id") clientQueryId: String,
      @PathVariable("format") exportFormat: ExportFormat,
      serverResponse: ServerHttpResponse
   ): Mono<Void> {
      return queryHistoryRecordRepository.findByClientQueryId(clientQueryId)
         .switchIfEmpty(Mono.defer { throw NotFoundException("No query with clientQueryId $clientQueryId found") })
         .flatMap { querySummary ->
            exportQueryResults(querySummary.queryId, exportFormat, serverResponse)
         }
   }

   @GetMapping("/api/query/history/clientId/{id}/profile")
   fun getQueryProfileDataFromClientId(@PathVariable("id") queryClientId: String): Mono<QueryProfileData> {
      return queryHistoryRecordRepository.findByClientQueryId(queryClientId)
         .switchIfEmpty(Mono.defer { throw NotFoundException("No query with clientQueryId $queryClientId found") })
         .flatMap { querySummary -> getQueryProfileData(querySummary) }
   }

   @GetMapping("/api/query/history/{id}/profile")
   fun getQueryProfileData(@PathVariable("id") queryId: String): Mono<QueryProfileData> {
      return queryHistoryRecordRepository.findByQueryId(queryId)
         .switchIfEmpty(Mono.defer { throw NotFoundException("No query with queryId $queryId found") })
         .flatMap { querySummary -> getQueryProfileData(querySummary) }
   }

   private fun getQueryProfileData(querySummary: QuerySummary) =
      lineageRecordRepository.findAllByQueryIdAndDataSourceType(
         querySummary.queryId,
         OperationResult.NAME
      ).collectList()
         .map { lineageRecords ->
            val remoteCalls =
               lineageRecords.map { objectMapper.readValue<OperationResult>(it.dataSourceJson).remoteCall }
            QueryProfileData(
               querySummary.queryId,
               querySummary.durationMs ?: 0,
               remoteCalls
            )
         }

   @PostMapping("/api/query/history/{id}/regressionPack")
   fun getRegressionPack( @PathVariable("id") queryId: String, @RequestBody request: RegressionPackRequest, response: ServerHttpResponse): Mono<Void> {

      val querySummary = queryHistoryRecordRepository.findByQueryId(queryId).toFuture()
      val results = queryResultRowRepository.findAllByQueryId(queryId).map { it.asTypeNamedInstance() }.collectList().toFuture()
      val lineageRecords = lineageRecordRepository.findAllByQueryId(queryId).collectList().toFuture()

      return response.writeByteArrays( regressionPackProvider.createRegressionPack(results.get(),querySummary.get(),lineageRecords.get(), request).toByteArray()  )
   }

}

fun ServerHttpResponse.writeByteArrays(bytes: ByteArray): Mono<Void> {
   val factory = this.bufferFactory()
   val dataBuffers = Flux.just(factory.wrap(bytes) )
   return this.writeWith(dataBuffers)
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

