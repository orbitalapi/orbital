package io.vyne.cask.rest

import arrow.core.flatMap
import arrow.core.getOrHandle
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.cask.CaskService
import io.vyne.cask.api.CaskApi
import io.vyne.cask.api.CaskIngestionResponse
import io.vyne.cask.ingest.IngestionInitialisedEvent
import io.vyne.cask.websocket.CaskWebsocketRequest
import io.vyne.utils.log
import org.springframework.context.ApplicationEventPublisher
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.InputStream

@RestController
class CaskRestController(private val caskService: CaskService,
                         private val applicationEventPublisher: ApplicationEventPublisher,
                         private val objectMapper: ObjectMapper = jacksonObjectMapper()) : CaskApi {

   override fun ingestMessage(
      contentType: String,
      typeReference: String,
      queryParams: MultiValueMap<String, String?>,
      input: String): CaskIngestionResponse {

      log().info("New ingestion request uri=/cask/${contentType}/${typeReference} queryParams=$queryParams")

      val requestOrError = caskService.resolveContentType(contentType)
         .flatMap { contentType ->
            caskService.resolveType(typeReference).map { versionedType ->
               CaskWebsocketRequest.create(contentType, versionedType, objectMapper, queryParams)
            }
         }

      return requestOrError
         .map { request ->
            applicationEventPublisher.publishEvent(IngestionInitialisedEvent(this, request.versionedType))
            val ingestionInput = Flux.just(input.byteInputStream() as InputStream)
            caskService.ingestRequest(request, ingestionInput)
               .count()
               .map { CaskIngestionResponse.success("Successfully ingested $it records") }
               .onErrorResume {
                  log().error("Ingestion error", it)
                  Mono.just(CaskIngestionResponse.rejected(it.toString()))
               }.block()
         }.getOrHandle { error ->
           CaskIngestionResponse.rejected(error.message)
         }
   }

   override fun getCasks() = caskService.getCasks()
   override fun getCaskDetails(tableName: String) = caskService.getCaskDetails(tableName)
   override fun deleteCask(tableName: String) =  caskService.deleteCask(tableName)
   override fun emptyCask(tableName: String)  = caskService.emptyCask(tableName)
}
