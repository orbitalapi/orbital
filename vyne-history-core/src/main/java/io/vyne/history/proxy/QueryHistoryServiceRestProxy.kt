package io.vyne.history.proxy

import com.google.common.net.HttpHeaders
import io.vyne.history.api.QueryHistoryServiceRestApi
import io.vyne.history.export.ExportFormat
import io.vyne.history.rest.QueryResultNodeDetail
import io.vyne.history.rest.RegressionPackRequest
import io.vyne.query.QueryProfileData
import io.vyne.query.ValueWithTypeName
import io.vyne.query.history.LineageRecord
import io.vyne.query.history.QuerySummary
import mu.KotlinLogging
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer

private val logger = KotlinLogging.logger {}
@RestController
class QueryHistoryServiceRestProxy(
   private val feignClient: QueryHistoryServiceRestApi,
   private val historyServiceAppName: String,
   private val discoveryClient: DiscoveryClient): QueryHistoryServiceRestApi by feignClient {

   @GetMapping("/api/query/history/{queryId}/{exportFormat}/export")
   fun exportQueryResults(@PathVariable queryId: String, @PathVariable exportFormat: ExportFormat, serverResponse: ServerHttpResponse): Mono<Void> {
      logger.info { "Exporting Query Results for $queryId" }
      val restUri = "/api/query/history/$queryId/$exportFormat/export"
     val dataBuffer =  WebClient
         .create(fetchHistoryServerAddress())
         .get()
         .uri(restUri)
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
        .accept(MediaType.APPLICATION_OCTET_STREAM)
         .retrieve()
        .bodyToFlux(DataBuffer::class.java)
      return serverResponse.writeWith(dataBuffer)
   }

   @GetMapping("/api/query/history/clientId/{id}/{format}/export")
   fun exportQueryResultsFromClientId(@PathVariable id: String, @PathVariable format: ExportFormat, serverResponse: ServerHttpResponse): Mono<Void> {
      val restUri = "/api/query/history/clientId/$id/$format/export"
      logger.info { "Exporting Query Results from client Query Id $id" }
      val dataBuffer =  WebClient
         .create(fetchHistoryServerAddress())
         .get()
         .uri(restUri)
         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
         .accept(MediaType.APPLICATION_OCTET_STREAM)
         .retrieve()
         .bodyToFlux(DataBuffer::class.java)
      return serverResponse.writeWith(dataBuffer)
   }

   @GetMapping("/api/query/history/{id}/export")
   fun exportQueryResultsToModelFormat(
      @PathVariable("id") queryId: String,
      serverResponse: ServerHttpResponse
   ): Mono<Void> {
      logger.info { "Exporting Query Results for $queryId" }
      val restUri = "/api/query/history/$queryId/export"
      val dataBuffer =  WebClient
         .create(fetchHistoryServerAddress())
         .get()
         .uri(restUri)
         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
         .accept(MediaType.APPLICATION_OCTET_STREAM)
         .retrieve()
         .bodyToFlux(DataBuffer::class.java)
      return serverResponse.writeWith(dataBuffer)
   }

   @GetMapping("/api/query/history/clientId/{id}/export")
   fun exportQueryResultsModelFormatFromClientId(
      @PathVariable("id") clientQueryId: String,
      serverResponse: ServerHttpResponse
   ): Mono<Void> {
      val restUri = "/api/query/history/clientId/$clientQueryId/export"
      logger.info { "Exporting Query Results from client Query Id $clientQueryId" }
      val dataBuffer =  WebClient
         .create(fetchHistoryServerAddress())
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

   override fun getHistoryRecordStream(queryId: String, limit: Long?): Flux<ValueWithTypeName> {
      logger.info { "fetching history record stream for $queryId" }
      return feignClient.getHistoryRecordStream(queryId, limit)
   }

   override fun getLineageRecord(dataSourceId: String): Mono<LineageRecord> {
      logger.info { "getting lineage record for data source $dataSourceId" }
      return feignClient.getLineageRecord(dataSourceId)
   }

   override fun getNodeDetail(queryId: String, rowValueHash: Int, attributePath: String): Mono<QueryResultNodeDetail> {
      logger.info { "getting node details for query Id $queryId, row hash $rowValueHash attribute path $attributePath" }
      return feignClient.getNodeDetail(queryId, rowValueHash, attributePath)
   }

   override fun getNodeDetailFromClientQueryId(clientQueryId: String, rowValueHash: Int, attributePath: String): Mono<QueryResultNodeDetail> {
      logger.info { "getting node details from client query id $clientQueryId, row hash $rowValueHash attribute path $attributePath" }
      return feignClient.getNodeDetailFromClientQueryId(clientQueryId, rowValueHash, attributePath)
   }

   override fun getQueryProfileData(queryId: String): Mono<QueryProfileData> {
      logger.info { "getting query profile data for id $queryId" }
      return feignClient.getQueryProfileData(queryId)
   }

   override fun getQueryProfileDataFromClientId(queryClientId: String): Mono<QueryProfileData> {
      logger.info { "getting query profile data for query client id $queryClientId" }
      return feignClient.getQueryProfileDataFromClientId(queryClientId)
   }

   override fun getQuerySummary(clientQueryId: String): Mono<QuerySummary> {
      logger.info { "Getting query summary for query client id $clientQueryId" }
      return feignClient.getQuerySummary(clientQueryId)
   }

   override fun getRegressionPack(queryId: String, request: RegressionPackRequest): Mono<ByteBuffer> {
      logger.info { "getting regression pack for query $queryId" }
      return feignClient.getRegressionPack(queryId, request)
   }

   override fun getRegressionPackFromClientId(clientQueryId: String, request: RegressionPackRequest): Mono<ByteBuffer> {
      logger.info { "getting regression pack for query client id $clientQueryId" }
      return feignClient.getRegressionPackFromClientId(clientQueryId, request)
   }

   override fun getRemoteCallResponse(remoteCallId: String): Mono<String> {
      logger.info { "getting remote call responses for call id $remoteCallId" }
      return feignClient.getRemoteCallResponse(remoteCallId)
   }

   override fun listHistory(): Flux<QuerySummary> {
      logger.info { "listing history" }
      return feignClient.listHistory()
   }
}
