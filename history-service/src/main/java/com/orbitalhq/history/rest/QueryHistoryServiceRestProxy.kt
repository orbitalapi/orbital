package com.orbitalhq.history.rest

import com.google.common.net.HttpHeaders
import com.orbitalhq.history.api.QueryHistoryServiceRestApi
import com.orbitalhq.history.api.QueryResultNodeDetail
import com.orbitalhq.history.api.RegressionPackRequest
import com.orbitalhq.history.rest.export.ExportFormat
import com.orbitalhq.query.QueryProfileData
import com.orbitalhq.query.ValueWithTypeName
import com.orbitalhq.query.history.LineageRecord
import com.orbitalhq.query.history.QuerySummary
import com.orbitalhq.security.VynePrivileges
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer

@ExcludeFromAnalyticsServer
@ConditionalOnProperty(prefix = "vyne.analytics", name = ["mode"], havingValue = "Remote", matchIfMissing = true)
@RestController
class QueryHistoryServiceRestProxy(
   private val feignClient: QueryHistoryServiceRestApi,
   private val historyServiceAppName: String = "vyne-analytics-server",
   private val discoveryClient: DiscoveryClient
): QueryHistoryServiceRestApi by feignClient {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   @GetMapping("/api/query/history/{queryId}/{exportFormat}/export")
   fun exportQueryResults(@PathVariable queryId: String, @PathVariable exportFormat: ExportFormat, serverResponse: ServerHttpResponse): Mono<Void> {
      logger.info { "Exporting Query Results for $queryId" }
      val restUri = "/api/query/history/$queryId/$exportFormat/export"
      val dataBuffer =  WebClient.create(fetchHistoryServerAddress())
         .get()
         .uri(restUri)
         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
         .accept(MediaType.APPLICATION_OCTET_STREAM)
         .retrieve()
         .bodyToFlux(DataBuffer::class.java)
      return serverResponse.writeWith(dataBuffer)
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   @GetMapping("/api/query/history/clientId/{id}/{format}/export")
   fun exportQueryResultsFromClientId(@PathVariable id: String, @PathVariable format: ExportFormat, serverResponse: ServerHttpResponse): Mono<Void> {
      val restUri = "/api/query/history/clientId/$id/$format/export"
      logger.info { "Exporting Query Results from client Query Id $id" }
      val dataBuffer =  WebClient.create(fetchHistoryServerAddress())
         .get()
         .uri(restUri)
         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
         .accept(MediaType.APPLICATION_OCTET_STREAM)
         .retrieve()
         .bodyToFlux(DataBuffer::class.java)
      return serverResponse.writeWith(dataBuffer)
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   @GetMapping("/api/query/history/{id}/export")
   fun exportQueryResultsToModelFormat(
      @PathVariable("id") queryId: String,
      serverResponse: ServerHttpResponse
   ): Mono<Void> {
      logger.info { "Exporting Query Results for $queryId" }
      val restUri = "/api/query/history/$queryId/export"
      val dataBuffer =  WebClient.create(fetchHistoryServerAddress())
         .get()
         .uri(restUri)
         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
         .accept(MediaType.APPLICATION_OCTET_STREAM)
         .retrieve()
         .bodyToFlux(DataBuffer::class.java)
      return serverResponse.writeWith(dataBuffer)
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   @GetMapping("/api/query/history/clientId/{id}/export")
   fun exportQueryResultsModelFormatFromClientId(
      @PathVariable("id") clientQueryId: String,
      serverResponse: ServerHttpResponse
   ): Mono<Void> {
      val restUri = "/api/query/history/clientId/$clientQueryId/export"
      logger.info { "Exporting Query Results from client Query Id $clientQueryId" }
      val dataBuffer =  WebClient.create(fetchHistoryServerAddress())
         .get()
         .uri(restUri)
         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
         .accept(MediaType.APPLICATION_OCTET_STREAM)
         .retrieve()
         .bodyToFlux(DataBuffer::class.java)
      return serverResponse.writeWith(dataBuffer)
   }

   private fun fetchHistoryServerAddress() = discoveryClient
      .getInstances(historyServiceAppName)
      .first()
      .uri
      .toASCIIString()

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   override fun getHistoryRecordStream(queryId: String, limit: Long?): Flux<ValueWithTypeName> {
      logger.info { "fetching history record stream for $queryId" }
      return feignClient.getHistoryRecordStream(queryId, limit)
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   override fun getLineageRecord(dataSourceId: String): Mono<LineageRecord> {
      logger.info { "getting lineage record for data source $dataSourceId" }
      return feignClient.getLineageRecord(dataSourceId)
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   override fun getNodeDetail(queryId: String, rowValueHash: Int, attributePath: String): Mono<QueryResultNodeDetail> {
      logger.info { "getting node details for query Id $queryId, row hash $rowValueHash attribute path $attributePath" }
      return feignClient.getNodeDetail(queryId, rowValueHash, attributePath)
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   override fun getNodeDetailFromClientQueryId(clientQueryId: String, rowValueHash: Int, attributePath: String): Mono<QueryResultNodeDetail> {
      logger.info { "getting node details from client query id $clientQueryId, row hash $rowValueHash attribute path $attributePath" }
      return feignClient.getNodeDetailFromClientQueryId(clientQueryId, rowValueHash, attributePath)
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   override fun getQueryProfileData(queryId: String): Mono<QueryProfileData> {
      logger.info { "getting query profile data for id $queryId" }
      return feignClient.getQueryProfileData(queryId)
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   override fun getQueryProfileDataFromClientId(queryClientId: String): Mono<QueryProfileData> {
      logger.info { "getting query profile data for query client id $queryClientId" }
      return feignClient.getQueryProfileDataFromClientId(queryClientId)
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   override fun getQuerySummary(clientQueryId: String): Mono<QuerySummary> {
      logger.info { "Getting query summary for query client id $clientQueryId" }
      return feignClient.getQuerySummary(clientQueryId)
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   override fun getRegressionPack(queryId: String, request: RegressionPackRequest): Mono<ByteBuffer> {
      logger.info { "getting regression pack for query $queryId" }
      return feignClient.getRegressionPack(queryId, request)
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   override fun getRegressionPackFromClientId(clientQueryId: String, request: RegressionPackRequest): Mono<ByteBuffer> {
      logger.info { "getting regression pack for query client id $clientQueryId" }
      return feignClient.getRegressionPackFromClientId(clientQueryId, request)
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
   override fun getRemoteCallResponse(remoteCallId: String): Mono<String> {
      logger.info { "getting remote call responses for call id $remoteCallId" }
      return feignClient.getRemoteCallResponse(remoteCallId)
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewQueryHistory}')")
   override fun listHistory(): Flux<QuerySummary> {
      logger.info { "listing history" }
      return feignClient.listHistory()
   }
}
