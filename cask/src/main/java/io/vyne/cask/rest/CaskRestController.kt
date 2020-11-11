package io.vyne.cask.rest

import arrow.core.getOrHandle
import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.cask.CaskIngestionRequest
import io.vyne.cask.CaskService
import io.vyne.cask.api.CaskApi
import io.vyne.cask.api.CaskIngestionErrorDtoPage
import io.vyne.cask.api.CaskIngestionErrorsRequestDto
import io.vyne.cask.api.CaskIngestionResponse
import io.vyne.cask.api.ContentType
import io.vyne.cask.api.CsvIngestionParameters
import io.vyne.cask.api.JsonIngestionParameters
import io.vyne.cask.api.XmlIngestionParameters
import io.vyne.cask.ingest.CaskIngestionErrorProcessor
import io.vyne.cask.ingest.IngestionInitialisedEvent
import io.vyne.cask.websocket.CsvWebsocketRequest
import io.vyne.cask.websocket.JsonWebsocketRequest
import io.vyne.cask.websocket.XmlWebsocketRequest
import io.vyne.utils.log
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationEventPublisher
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.InputStream
import java.nio.charset.Charset

@RestController
class CaskRestController(private val caskService: CaskService,
                         private val applicationEventPublisher: ApplicationEventPublisher,
                         private val caskIngestionErrorProcessor: CaskIngestionErrorProcessor,
                         @Qualifier("ingesterMapper") private val mapper: ObjectMapper) : CaskApi {

   // Workaround for feign not supporting pojos for RequestParam
   override fun ingestCsv(
      typeReference: String,
      delimiter: Char,
      firstRecordAsHeader: Boolean,
      nullValue: Set<String>,
      ignoreContentBefore: String?,
      containsTrailingDelimiters: Boolean,
      debug: Boolean,
      input: String
   ): CaskIngestionResponse {
      val parameters = CsvIngestionParameters(
         delimiter,
         firstRecordAsHeader,
         nullValue,
         ignoreContentBefore,
         containsTrailingDelimiters,
         debug
      )
      return ingestCsv(typeReference, parameters, input)
   }

   private fun ingestCsv(typeReference: String, parameters: CsvIngestionParameters, input: String): CaskIngestionResponse {
      log().info("New csv ingestion request for type $typeReference with config $parameters")
      return caskService.resolveType(typeReference).map { versionedType ->
         val request = CsvWebsocketRequest(parameters, versionedType, caskIngestionErrorProcessor)
         // TODO : Input should be an InputStream already.
         val inputStream = Flux.just(input.byteInputStream() as InputStream)
         // TODO : How to avoid this blocking?
         ingestRequest(request, inputStream).block()
      }.getOrHandle { error ->
         CaskIngestionResponse.rejected(error.message)
      }
   }

   // Workaround for feign not supporting pojos for RequestParam
   override fun ingestJson(typeReference: String, debug: Boolean, input: String): CaskIngestionResponse {
      val parameters = JsonIngestionParameters(debug)
      return ingestJson(typeReference, parameters, input)
   }

   override fun ingestXml(typeReference: String, debug: Boolean, elementSelector: String?, input: String): CaskIngestionResponse {
      val xmlIngestionParameters = XmlIngestionParameters(debug, elementSelector)
      return caskService.resolveType(typeReference).map { versionedType ->
         val request = XmlWebsocketRequest(xmlIngestionParameters, versionedType)
         val inputStream = Flux.just(input.byteInputStream() as InputStream)
         ingestRequest(request, inputStream).block()
      }.getOrHandle { error ->
         CaskIngestionResponse.rejected(error.message)
      }
   }

   private fun ingestJson(typeReference: String, parameters: JsonIngestionParameters, input: String): CaskIngestionResponse {
      return caskService.resolveType(typeReference).map { versionedType ->
         val request = JsonWebsocketRequest(parameters, versionedType, mapper)
         val inputStream = Flux.just(input.byteInputStream() as InputStream)
         // TODO : How to avoid this blocking?
         ingestRequest(request, inputStream).block()
      }.getOrHandle { error ->
         CaskIngestionResponse.rejected(error.message)
      }
   }

   private fun ingestRequest(request: CaskIngestionRequest, ingestionInput: Flux<InputStream>): Mono<CaskIngestionResponse> {
      applicationEventPublisher.publishEvent(IngestionInitialisedEvent(this, request.versionedType))

      return caskService.ingestRequest(request, ingestionInput)
         .count()
         .map { CaskIngestionResponse.success("Successfully ingested $it records") }
         .onErrorResume {
            log().error("Ingestion error", it)
            Mono.just(CaskIngestionResponse.rejected(it.toString()))
         }
   }

   override fun getCasks() = caskService.getCasks()
   override fun getCaskDetails(tableName: String) = caskService.getCaskDetails(tableName)
   override fun deleteCask(tableName: String) = caskService.deleteCask(tableName)
   override fun emptyCask(tableName: String) = caskService.emptyCask(tableName)
   override fun getCaskIngestionErrors(tableName: String, request: CaskIngestionErrorsRequestDto): CaskIngestionErrorDtoPage {
      log().info("Searching ingestion errors for $tableName with criteria $request")
      return caskService.caskIngestionErrorsFor(
         tableName,
         request.pageNumber,
         request.pageSize,
         request.searchStart,
         request.searchEnd)
   }

   override fun getIngestionMessage(caskMessageId: String): ResponseEntity<Resource> {
      val (resource, contentType) = caskService.caskIngestionMessage(caskMessageId)
      val (mediaType, fileName) = when(contentType) {
         ContentType.xml -> MediaType.APPLICATION_XML to "$caskMessageId.xml"
         ContentType.csv -> MediaType("text", "csv", Charset.forName("utf-8")) to "$caskMessageId.csv"
         ContentType.json -> MediaType.APPLICATION_JSON to "$caskMessageId.json"
         else -> MediaType.APPLICATION_OCTET_STREAM to "$caskMessageId"
      }
      val builder = ResponseEntity.ok()
         .contentType(mediaType)
         .header(
            HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
            HttpHeaders.CONTENT_DISPOSITION
         )
      builder.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$fileName\"")
      return builder.body(resource)
   }

   override fun deleteCaskByTypeName(typeName: String) {
      caskService.deleteCaskByTypeName(typeName)
   }
}

