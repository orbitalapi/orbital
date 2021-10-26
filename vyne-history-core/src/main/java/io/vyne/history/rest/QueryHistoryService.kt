package io.vyne.history.rest

import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.vyne.ExceptionProvider
import io.vyne.history.ExportFormat
import io.vyne.history.QueryHistoryConfig
import io.vyne.history.QueryHistoryExporter
import io.vyne.history.QueryHistoryResultNodeFinder
import io.vyne.history.RegressionPackProvider
import io.vyne.history.RemoteCallAnalyzer
import io.vyne.history.db.LineageRecordRepository
import io.vyne.history.db.QueryHistoryRecordRepository
import io.vyne.history.db.QueryResultRowRepository
import io.vyne.history.db.QuerySankeyChartRowRepository
import io.vyne.history.db.RemoteCallResponseRepository
import io.vyne.models.OperationResult
import io.vyne.query.QueryProfileData
import io.vyne.query.ValueWithTypeName
import io.vyne.query.history.LineageRecord
import io.vyne.query.history.QuerySankeyChartRow
import io.vyne.query.history.QuerySummary
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.fqn
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactor.asFlux
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import java.time.Duration
import java.util.concurrent.CompletableFuture

@FlowPreview
@RestController
class QueryHistoryService(
   private val queryHistoryRecordRepository: QueryHistoryRecordRepository,
   private val queryResultRowRepository: QueryResultRowRepository,
   private val lineageRecordRepository: LineageRecordRepository,
   private val remoteCallResponseRepository: RemoteCallResponseRepository,
   private val sankeyChartRowRepository: QuerySankeyChartRowRepository,
   private val queryHistoryExporter: QueryHistoryExporter,
   private val objectMapper: ObjectMapper,
   private val regressionPackProvider: RegressionPackProvider,
   private val queryHistoryConfig: QueryHistoryConfig,
   private val exceptionProvider: ExceptionProvider
) {
   private val remoteCallAnalyzer = RemoteCallAnalyzer()

   @DeleteMapping("/api/query/history")
   fun clearHistory() {
      queryHistoryRecordRepository.deleteAll()
   }

   @GetMapping("/api/query/history")
   fun listHistory(): Flux<QuerySummary> {

      return queryHistoryRecordRepository
         .findAllByOrderByStartTimeDesc(PageRequest.of(0, queryHistoryConfig.pageSize)).toFlux().flatMap {
            Mono.zip(
               Mono.just(it),
               Mono.just(queryResultRowRepository.countAllByQueryId(it.queryId))
            ) { querySummaryRecord: QuerySummary, recordCount: Int ->
               val derivedRecordCount = when {
                  querySummaryRecord.recordCount == null -> recordCount
                  recordCount == 0 && querySummaryRecord.recordCount != null -> querySummaryRecord.recordCount
                  else -> recordCount
               }
               querySummaryRecord.recordCount = derivedRecordCount
               querySummaryRecord.durationMs = querySummaryRecord.endTime?.let { Duration.between(querySummaryRecord.startTime, querySummaryRecord.endTime).toMillis() }
               querySummaryRecord }
         }
   }

   @GetMapping("/api/query/history/summary/clientId/{clientId}")
   fun getQuerySummary(@PathVariable("clientId") clientQueryId: String):QuerySummary {
      return queryHistoryRecordRepository.findByClientQueryId(clientQueryId)
   }

   @GetMapping("/api/query/history/calls/{remoteCallId}")
   fun getRemoteCallResponse(@PathVariable("remoteCallId") remoteCallId: String): String {
      if (!queryHistoryConfig.persistRemoteCallResponses) {
        throw exceptionProvider.badRequestException(
            "Capturing remote call responses has been disabled.  To enable, please configure setting vyne.history.persistRemoteCallResponses.")
      }

      val strings =  remoteCallResponseRepository.findAllByRemoteCallId(remoteCallId)
         .map { remoteCallResponse -> remoteCallResponse.response }

      return  if (strings.size == 1) {
         strings.first()
      } else {
         strings.joinToString(prefix = "[", postfix = "]")
      }

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
   ): Flux<ValueWithTypeName> {
      val querySummary = queryHistoryRecordRepository.findByQueryId(queryId)

      return querySummary.let { querySummary ->
         val flux = if (limit != null) {
            this.queryResultRowRepository.findAllByQueryId(queryId)
               .take(limit.toInt())
         } else {
            this.queryResultRowRepository.findAllByQueryId(queryId)
         }
         var firstRowEmitted = false
         flux.mapNotNull { resultRow ->
            val typeNamedInstance = resultRow.asTypeNamedInstance(objectMapper)
            // Only include anonymous type data on the first emitted entry, as they're
            // really expensive to send
            val anonymousTypes = when {
               firstRowEmitted -> ValueWithTypeName.NO_ANONYMOUS_TYPES
               !firstRowEmitted -> {
                  firstRowEmitted = true
                  querySummary.anonymousTypesJson
                     ?: ValueWithTypeName.NO_ANONYMOUS_TYPES
               }
               else -> error("This shouldn't happen")
            }
            val record = ValueWithTypeName(
               typeNamedInstance.typeName.fqn(),
               anonymousTypes,
               typeNamedInstance.convertToRaw()!!,
               resultRow.valueHash,
               resultRow.queryId
            )
            firstRowEmitted = true
            record
         }
      }.toFlux()


   }

   @GetMapping("/api/query/history/clientId/{id}/dataSource/{rowId}/{attributePath}")
   fun getNodeDetailFromClientQueryId(
      @PathVariable("id") clientQueryId: String,
      @PathVariable("rowId") rowValueHash: Int,
      @PathVariable("attributePath") attributePath: String
   ): QueryResultNodeDetail {
      return queryHistoryRecordRepository.findByClientQueryId(clientQueryId)
         .let { querySummary ->
            getNodeDetail(querySummary.queryId, rowValueHash, attributePath)
         }
   }

   @GetMapping("/api/query/history/{id}/dataSource/{rowId}/{attributePath}")
   fun getNodeDetail(
      @PathVariable("id") queryId: String,
      @PathVariable("rowId") rowValueHash: Int,
      @PathVariable("attributePath") attributePath: String
   ): QueryResultNodeDetail {
      val resultRow = queryResultRowRepository.findByQueryIdAndValueHash(queryId, rowValueHash).first()
      val typeNamedInstance = resultRow.asTypeNamedInstance(objectMapper)
      val nodeParts = attributePath.split(".")
      val nodeDetail = QueryHistoryResultNodeFinder.find(nodeParts, typeNamedInstance, attributePath, exceptionProvider)
      if (nodeDetail.dataSourceId == null) {
         error("No dataSourceId is present on TypeNamedInstance for query $queryId row $rowValueHash path $attributePath")
      }

      return lineageRecordRepository.findById(nodeDetail.dataSourceId)
         .map { lineageRecord -> nodeDetail.copy(source = lineageRecord.dataSourceJson) }.get()

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
      return exportQueryResults(queryHistoryRecordRepository.findByClientQueryId(clientQueryId).queryId, exportFormat, serverResponse)
   }

   @GetMapping("/api/query/history/clientId/{id}/profile")
   fun getQueryProfileDataFromClientId(@PathVariable("id") queryClientId: String): QueryProfileData {
      return getQueryProfileData(queryHistoryRecordRepository.findByClientQueryId(queryClientId))
   }

   @GetMapping("/api/query/history/{id}/profile")
   fun getQueryProfileData(@PathVariable("id") queryId: String): QueryProfileData {
      try {
         return getQueryProfileData(queryHistoryRecordRepository.findByQueryId(queryId))
      } catch (execption : EmptyResultDataAccessException) {
         throw exceptionProvider.notFoundException("Query Id ${queryId} could not be found")
      }
   }

   @GetMapping("/api/query/history/dataSource/{id}")
   fun getLineageRecord(@PathVariable("id") dataSourceId: String): LineageRecord {
      return lineageRecordRepository.findById(dataSourceId).orElseThrow {
         exceptionProvider.notFoundException("No dataSource with id $dataSourceId found" )
      }
   }

   @GetMapping("/api/query/history/{id}/sankey")
   fun getQuerySankeyView(@PathVariable("id") queryId: String): List<QuerySankeyChartRow> {
      return sankeyChartRowRepository.findAllByQueryId(queryId)
   }

   @GetMapping("/api/query/history/clientId/{id}/sankey")
   fun getQuerySankeyViewFromClientQueryId(@PathVariable("id") queryClientId: String): List<QuerySankeyChartRow> {
      val querySummary = queryHistoryRecordRepository.findByClientQueryId(queryClientId)
      return sankeyChartRowRepository.findAllByQueryId(querySummary.queryId)
   }

   private fun getQueryProfileData(querySummary: QuerySummary): QueryProfileData {
      val lineageRecords =  lineageRecordRepository.findAllByQueryIdAndDataSourceType(
         querySummary.queryId,
         OperationResult.NAME
      )

      val remoteCalls = lineageRecords.map { objectMapper.readValue<OperationResult>(it.dataSourceJson).remoteCall }
      val stats = remoteCallAnalyzer.generateStats(remoteCalls)
      val queryLineageData = sankeyChartRowRepository.findAllByQueryId(querySummary.queryId)

      return QueryProfileData(
         querySummary.queryId,
         querySummary.durationMs ?: 0,
         remoteCalls,
         operationStats = stats,
         queryLineageData =  queryLineageData
      )

   }

   @PostMapping("/api/query/history/clientId/{id}/regressionPack")
   fun getRegressionPackFromClientId(
      @PathVariable("id") clientQueryId: String,
      @RequestBody request: RegressionPackRequest,
      response: ServerHttpResponse
   ): Mono<Void> {
      val querySummary = queryHistoryRecordRepository.findByClientQueryId(clientQueryId)
      return getRegressionPack(querySummary.queryId, request, response)
   }

   @PostMapping("/api/query/history/{id}/regressionPack")
   fun getRegressionPack(
      @PathVariable("id") queryId: String,
      @RequestBody request: RegressionPackRequest,
      response: ServerHttpResponse
   ): Mono<Void> {

      if (!queryHistoryConfig.persistRemoteCallResponses) {
         throw exceptionProvider.badRequestException("Capturing remote call responses has been disabled.  Please configure setting vyne.history.persistRemoteCallResponses and set to true to enable the creation of regression packs.")
      }

      val querySummary = CompletableFuture.supplyAsync { queryHistoryRecordRepository.findByQueryId(queryId) }
      val results =
         CompletableFuture.supplyAsync { queryResultRowRepository.findAllByQueryId(queryId).map { it.asTypeNamedInstance() } }
      val lineageRecords = CompletableFuture.supplyAsync { lineageRecordRepository.findAllByQueryId(queryId) }
      val remoteCalls = CompletableFuture.supplyAsync { remoteCallResponseRepository.findAllByQueryId(queryId) }

      return response.writeByteArrays(
         regressionPackProvider.createRegressionPack(
            results.get(),
            querySummary.get()!!,
            lineageRecords.get(),
            remoteCalls.get(),
            request
         ).toByteArray()
      )
   }

   @GetMapping("/api/query/history/filter/{responseType}")
   fun fetchAllQueriesReturnType(@PathVariable("responseType") fullyQualifiedTypeName: String): Mono<QueryList> {
         val queries = queryHistoryRecordRepository
            .findAllByResponseType(fullyQualifiedTypeName)
            .mapNotNull { it.taxiQl ?: it.queryJson }

      return Mono.just(QueryList(fullyQualifiedTypeName, queries))

   }
}

fun ServerHttpResponse.writeByteArrays(bytes: ByteArray): Mono<Void> {
   val factory = this.bufferFactory()
   val dataBuffers = Flux.just(factory.wrap(bytes))
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

data class QueryList(val responseType: String, val queries: List<String>)
