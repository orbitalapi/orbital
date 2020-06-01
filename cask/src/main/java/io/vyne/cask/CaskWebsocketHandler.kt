package io.vyne.cask

import arrow.core.*
import arrow.core.extensions.either.applicativeError.handleError
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.cask.api.CaskIngestionResponse
import io.vyne.cask.ingest.IngestionInitialisedEvent
import io.vyne.cask.websocket.CaskWebsocketRequest
import io.vyne.cask.websocket.contentType
import io.vyne.cask.websocket.typeReference
import io.vyne.utils.log
import io.vyne.utils.orElse
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.CloseStatus.NOT_ACCEPTABLE
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class CaskWebsocketHandler(
   val caskService: CaskService,
   val applicationEventPublisher: ApplicationEventPublisher,
   val mapper: ObjectMapper = jacksonObjectMapper()) : WebSocketHandler {

   override fun handle(session: WebSocketSession): Mono<Void> {
      log().info("Opening new sessionId=${session.id} uri=${session.handshakeInfo.uri}")

      val requestOrError = caskService.resolveContentType(session.contentType())
         .flatMap { contentType ->
            caskService.resolveType(session.typeReference()).map { versionedType ->
               CaskWebsocketRequest.create(session, contentType, versionedType, mapper)
            }
         }

      return requestOrError
         .map { request ->
            applicationEventPublisher.publishEvent(IngestionInitialisedEvent(this, request.versionedType))
            ingestMessages(request)
         }.getOrHandle { error ->
            log().info("Closing sessionId=${session.id}.  Error: ${error.message}")
            session.close(CloseStatus(NOT_ACCEPTABLE.code, error.message)).then()
         }
   }

   private fun ingestMessages(request: CaskWebsocketRequest): Mono<Void> {
      val output: EmitterProcessor<WebSocketMessage> = EmitterProcessor.create()
      val outputSink = output.sink()

      // i don't like this, it's pretty ugly
      // partially because exceptions are thrown outside flux pipelines
      // we have to refactor code behind ingestion to fix this problem
      val session = request.session
      session.receive()
         .name("cask_ingestion_request")
         // This will register timer with the above name
         // Registry instance is auto-detected
         // Percentiles are configured globally for all the timers, see CaskApp
         .metrics()
         .map {
            log().info("Ingesting message from sessionId=${request.session.id}")
            try {
               caskService
                  .ingestRequest(request, Flux.just(it.payload.asInputStream()))
                  .count()
                  .map { "Successfully ingested ${it} records" }
                  .subscribe(
                     { result ->
                        log().info("Successfully ingested message from sessionId=${session.id}")
                        if (request.debug()) {
                           outputSink.next(successResponse(session, result))
                        }
                     },
                     { error ->
                        log().error("Error ingesting message from sessionId=${session.id}", error)
                        outputSink.next(errorResponse(session, extractError(error)))
                     }
                  )
            } catch (error: Exception) {
               log().error("Error ingesting message from sessionId=${session.id}", error)
               outputSink.next(errorResponse(session, extractError(error)))
            }
         }
         .doOnComplete {
            log().info("Closing sessionId=${session.id}")
            output.onComplete()
         }
         .doOnError { error ->
            log().error("Error ingesting message from sessionId=${session.id}", error)
         }
         .subscribe()

      return session.send(output)
   }

   private fun extractError(error: Throwable): String {
      return when (error.cause) {
         // This can leak some of the internal data structures/classes
         is InvalidFormatException -> error.message.orElse("An InvalidFormatException was thrown, but no further details are available.")
         is IllegalArgumentException -> error.message.orElse("An IllegalArgumentException was thrown, but no further details are available.")
         is JsonParseException -> error.message.orElse("Malformed JSON message")
         else -> error.message.orElse("Unexpected ingestion error.")
      }
   }

   private fun successResponse(session: WebSocketSession, message: String): WebSocketMessage {
      val msg = CaskIngestionResponse.success(message)
      val json = mapper.writeValueAsString(msg)
      return session.textMessage(json)
   }

   private fun errorResponse(session: WebSocketSession, errorMessage: String): WebSocketMessage {
      val msg = CaskIngestionResponse.rejected(errorMessage)
      val json = mapper.writeValueAsString(msg)
      return session.textMessage(json)
   }
}
