package io.vyne.history.rest

import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.kotlin.readValue
import io.vyne.history.KeepAsJsonDeserializer
import io.vyne.history.QueryAnalyticsConfig
import io.vyne.history.QueryHistoryResultNodeFinder
import io.vyne.history.RemoteCallAnalyzer
import io.vyne.history.api.QueryHistoryServiceRestApi
import io.vyne.history.db.LineageRecordRepository
import io.vyne.history.db.QueryHistoryRecordRepository
import io.vyne.history.db.QueryResultRowRepository
import io.vyne.history.db.QuerySankeyChartRowRepository
import io.vyne.history.db.RemoteCallResponseRepository
import io.vyne.history.export.ExportFormat
import io.vyne.history.export.QueryHistoryExporter
import io.vyne.history.export.RegressionPackProvider
import io.vyne.models.OperationResult
import io.vyne.query.QueryProfileData
import io.vyne.query.ValueWithTypeName
import io.vyne.query.history.LineageRecord
import io.vyne.query.history.QuerySankeyChartRow
import io.vyne.query.history.QuerySummary
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.fqn
import io.vyne.security.VynePrivileges
import io.vyne.utils.ExceptionProvider
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactor.asFlux
import mu.KotlinLogging
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.access.prepost.PreAuthorize
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
import java.nio.ByteBuffer
import java.time.Duration
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

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
   private val queryAnalyticsConfig: QueryAnalyticsConfig,
   private val exceptionProvider: ExceptionProvider
) : QueryHistoryServiceRestApi {
   private val remoteCallAnalyzer = RemoteCallAnalyzer()

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   @DeleteMapping("/api/query/history")
   fun clearHistory() {
      queryHistoryRecordRepository.deleteAll()
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewQueryHistory}')")
   @GetMapping("/api/query/history")
   override fun listHistory(): Flux<QuerySummary> {

      return queryHistoryRecordRepository
         .findAllByOrderByStartTimeDesc(PageRequest.of(0, queryAnalyticsConfig.pageSize)).toFlux().flatMap {
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
               querySummaryRecord.durationMs = querySummaryRecord.endTime?.let {
                  Duration.between(
                     querySummaryRecord.startTime,
                     querySummaryRecord.endTime
                  ).toMillis()
               }
               querySummaryRecord
            }
         }
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   @GetMapping("/api/query/history/summary/clientId/{clientId}")
   override fun getQuerySummary(@PathVariable("clientId") clientQueryId: String): Mono<QuerySummary> {
      logger.info { "Getting query summary for query client id $clientQueryId" }
      return queryHistoryRecordRepository.findByClientQueryId(clientQueryId)?.let {
         Mono.just(it)
      } ?: Mono.empty()
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   @GetMapping("/api/query/history/calls/{remoteCallId}")
   override fun getRemoteCallResponse(@PathVariable("remoteCallId") remoteCallId: String): Mono<String> {
      logger.info { "getting remote call responses for call id $remoteCallId" }
      if (!queryAnalyticsConfig.persistRemoteCallResponses) {
         throw exceptionProvider.badRequestException(
            "Capturing remote call responses has been disabled.  To enable, please configure setting vyne.analytics.persistRemoteCallResponses."
         )
      }

      val strings = remoteCallResponseRepository.findAllByRemoteCallId(remoteCallId)
         .map { remoteCallResponse -> remoteCallResponse.response }

      val just = if (strings.size == 1) {
         strings.first()
      } else {
         strings.joinToString(prefix = "[", postfix = "]")
      }

      return Mono.just(just)

   }


   /**
    * Returns the results (as JSON of TypeNamedInstances) over server-sent-events
    */
   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   @GetMapping(
      "/api/query/history/{id}/results", produces = [
         MediaType.TEXT_EVENT_STREAM_VALUE,
         MediaType.APPLICATION_JSON_VALUE,
      ]
   )
   override fun getHistoryRecordStream(
      @PathVariable("id") queryId: String,
      @RequestParam("limit", required = false) limit: Long?
   ): Flux<ValueWithTypeName> {
      logger.info { "fetching history record stream for $queryId" }
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
   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   override fun getNodeDetailFromClientQueryId(
      @PathVariable("id") clientQueryId: String,
      @PathVariable("rowId") rowValueHash: Int,
      @PathVariable("attributePath") attributePath: String
   ): Mono<QueryResultNodeDetail> {
      logger.info { "getting node details from client query id $clientQueryId, row hash $rowValueHash attribute path $attributePath" }

      return queryHistoryRecordRepository.findByClientQueryId(clientQueryId)
         ?.let { querySummary ->
            getNodeDetail(querySummary.queryId, rowValueHash, attributePath)
         } ?: Mono.empty()
   }

   @GetMapping("/api/query/history/{id}/dataSource/{rowId}/{attributePath}")
   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   override fun getNodeDetail(
      @PathVariable("id") queryId: String,
      @PathVariable("rowId") rowValueHash: Int,
      @PathVariable("attributePath") attributePath: String
   ): Mono<QueryResultNodeDetail> {
      logger.info { "getting node details for query Id $queryId, row hash $rowValueHash attribute path $attributePath" }
      val resultRow = queryResultRowRepository.findByQueryIdAndValueHash(queryId, rowValueHash).first()
      val typeNamedInstance = resultRow.asTypeNamedInstance(objectMapper)
      val nodeParts = attributePath.split(".")
      val nodeDetail = QueryHistoryResultNodeFinder.find(nodeParts, typeNamedInstance, attributePath, exceptionProvider)
      if (nodeDetail.dataSourceId == null) {
         error("No dataSourceId is present on TypeNamedInstance for query $queryId row $rowValueHash path $attributePath")
      }

      val linegaeRecord = lineageRecordRepository.findByQueryIdAndDataSourceId(queryId, nodeDetail.dataSourceId)
         .map { lineageRecord -> nodeDetail.copy(source = lineageRecord.dataSourceJson) }.get()
      return Mono.just(linegaeRecord)
   }

   @GetMapping("/api/query/history/{id}/{format}/export")
   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
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
   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   fun exportQueryResultsFromClientId(
      @PathVariable("id") clientQueryId: String,
      @PathVariable("format") exportFormat: ExportFormat,
      serverResponse: ServerHttpResponse
   ): Mono<Void> {
      val querySummary = queryHistoryRecordRepository.findByClientQueryId(clientQueryId)
      return querySummary?.let { exportQueryResults(it.queryId, exportFormat, serverResponse) } ?: Mono.empty()
   }

   @GetMapping("/api/query/history/{id}/export")
   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   fun exportQueryResultsToModelFormat(
      @PathVariable("id") queryId: String,
      serverResponse: ServerHttpResponse
   ): Mono<Void> {
      val bufferFactory = serverResponse.bufferFactory()
      val dataBuffers = queryHistoryExporter.export(queryId, ExportFormat.CUSTOM)
         .asFlux()
         .map { bufferFactory.wrap(it.toString().toByteArray()) }
      return serverResponse.writeWith(dataBuffers)
   }

   @GetMapping("/api/query/history/clientId/{id}/export")
   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   fun exportQueryResultsModelFormatFromClientId(
      @PathVariable("id") clientQueryId: String,
      serverResponse: ServerHttpResponse
   ): Mono<Void> {
      val querySummary = queryHistoryRecordRepository.findByClientQueryId(clientQueryId)
      return querySummary?.let {
         exportQueryResults(it.queryId, ExportFormat.CUSTOM, serverResponse)
      } ?: Mono.empty()
   }


   @GetMapping("/api/query/history/clientId/{id}/profile")
   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   override fun getQueryProfileDataFromClientId(@PathVariable("id") queryClientId: String): Mono<QueryProfileData> {
      logger.info { "getting query profile data for query client id $queryClientId" }
      return queryHistoryRecordRepository.findByClientQueryId(queryClientId)?.let {
         getQueryProfileData(it)
      } ?: Mono.empty()
   }

   @GetMapping("/api/query/history/{id}/profile")
   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   override fun getQueryProfileData(@PathVariable("id") queryId: String): Mono<QueryProfileData> {
      logger.info { "getting query profile data for id $queryId" }
      try {
         return getQueryProfileData(queryHistoryRecordRepository.findByQueryId(queryId))
      } catch (execption: EmptyResultDataAccessException) {
         throw exceptionProvider.notFoundException("Query Id $queryId could not be found")
      }
   }

   @GetMapping("/api/query/history/dataSource/{id}")
   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   override fun getLineageRecord(@PathVariable("id") dataSourceId: String): Mono<LineageRecord> {
      logger.info { "getting lineage record for data source $dataSourceId" }
      // Technically, data sources can belong to multiple queries, which is why this is a find-all.
      // However, they're generally the same.  So just take the first for now.
      val linegaeRecord = lineageRecordRepository.findAllByDataSourceId(dataSourceId)
         .firstOrNull() ?: throw exceptionProvider.notFoundException("No dataSource with id $dataSourceId found")
      return Mono.just(linegaeRecord)
   }

   @GetMapping("/api/query/history/{id}/sankey")
   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   fun getQuerySankeyView(@PathVariable("id") queryId: String): List<QuerySankeyChartRow> {
      return sankeyChartRowRepository.findAllByQueryId(queryId)
   }

   @GetMapping("/api/query/history/clientId/{id}/sankey")
   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   fun getQuerySankeyViewFromClientQueryId(@PathVariable("id") queryClientId: String): List<QuerySankeyChartRow> {
      val querySummary = queryHistoryRecordRepository.findByClientQueryId(queryClientId)
      return querySummary?.let { sankeyChartRowRepository.findAllByQueryId(it.queryId) } ?: listOf()
   }


   private fun getQueryProfileData(querySummary: QuerySummary): Mono<QueryProfileData> {
      val lineageRecords = lineageRecordRepository.findAllByQueryIdAndDataSourceType(
         querySummary.queryId,
         OperationResult.NAME
      )

      val remoteCalls = lineageRecords.map { objectMapper.readValue<OperationResult>(it.dataSourceJson).remoteCall }
      val stats = remoteCallAnalyzer.generateStats(remoteCalls)
      val queryLineageData = sankeyChartRowRepository.findAllByQueryId(querySummary.queryId)

      return Mono.just(
         QueryProfileData(
            querySummary.queryId,
            querySummary.durationMs ?: 0,
            remoteCalls,
            operationStats = stats,
            queryLineageData = queryLineageData
         )
      )
   }

   @PostMapping("/api/query/history/clientId/{id}/regressionPack")
   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   override fun getRegressionPackFromClientId(
      @PathVariable("id") clientQueryId: String,
      @RequestBody request: RegressionPackRequest
   ): Mono<ByteBuffer> {
      val querySummary = queryHistoryRecordRepository.findByClientQueryId(clientQueryId)
      return querySummary?.let {
         getRegressionPack(it.queryId, request)
      } ?: Mono.empty()
   }

   @PostMapping("/api/query/history/{id}/regressionPack")
   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   override fun getRegressionPack(
      @PathVariable("id") queryId: String,
      @RequestBody request: RegressionPackRequest
   ): Mono<ByteBuffer> {

      if (!queryAnalyticsConfig.persistRemoteCallResponses) {
         throw exceptionProvider.badRequestException("Capturing remote call responses has been disabled.  Please configure setting vyne.history.persistRemoteCallResponses and set to true to enable the creation of regression packs.")
      }

      val querySummary = CompletableFuture.supplyAsync { queryHistoryRecordRepository.findByQueryId(queryId) }
      val results =
         CompletableFuture.supplyAsync {
            queryResultRowRepository.findAllByQueryId(queryId).map { it.asTypeNamedInstance() }
         }
      val lineageRecords = CompletableFuture.supplyAsync { lineageRecordRepository.findAllByQueryId(queryId) }
      val remoteCalls = CompletableFuture.supplyAsync { remoteCallResponseRepository.findAllByQueryId(queryId) }

      val regressionPackFuture = CompletableFuture
         .allOf(querySummary, results, lineageRecords, remoteCalls)
         .thenApply {
            ByteBuffer.wrap(
               regressionPackProvider.createRegressionPack(
                  results.join(),
                  querySummary.join(),
                  lineageRecords.join(),
                  remoteCalls.join(),
                  request
               ).toByteArray()
            )
         }
      return Mono.fromFuture(regressionPackFuture)
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
   @JsonDeserialize(using = KeepAsJsonDeserializer::class)
   val source: String? // json of the DataSource
)

data class QueryList(val responseType: String, val queries: List<String>)
