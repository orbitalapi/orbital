package io.vyne.cask.websocket

import arrow.core.*
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.cask.CaskService
import io.vyne.cask.api.CaskIngestionResponse
import io.vyne.cask.ingest.IngestionInitialisedEvent
import io.vyne.utils.log
import io.vyne.utils.orElse
import org.springframework.beans.factory.annotation.Qualifier
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
   @Qualifier("ingesterMapper") val mapper: ObjectMapper) : WebSocketHandler {

   override fun handle(session: WebSocketSession): Mono<Void> {
      log().info("Opening new sessionId=${session.id} uri=${session.handshakeInfo.uri}")

      val requestOrError = caskService.resolveContentType(session.contentType())
         .flatMap { contentType ->
            caskService.resolveType(session.typeReference()).map { versionedType ->
               CaskWebsocketRequest.create(contentType, versionedType, mapper, session.queryParams())
            }
         }

      return requestOrError
         .map { request ->
            applicationEventPublisher.publishEvent(IngestionInitialisedEvent(this, request.versionedType))
            ingestMessages(session, request)
         }.getOrHandle { error ->
            log().info("Closing sessionId=${session.id}.  Error: ${error.message}")
            session.close(CloseStatus(NOT_ACCEPTABLE.code, error.message)).then()
         }
   }

   private fun ingestMessages(session: WebSocketSession, request: CaskWebsocketRequest): Mono<Void> {
      val output: EmitterProcessor<WebSocketMessage> = EmitterProcessor.create()
      val outputSink = output.sink()

      // i don't like this, it's pretty ugly
      // partially because exceptions are thrown outside flux pipelines
      // we have to refactor code behind ingestion to fix this problem
      session.receive()
         .name("cask_ingestion_request")
         // This will register timer with the above name
         // Registry instance is auto-detected
         // Percentiles are configured globally for all the timers, see CaskApp
         .metrics()
         .map { message ->
            log().info("Ingesting message from sessionId=${session.id}")
            try {
               val containsHeader = request.params.getParam("firstRowAsHeader").orElse(false) as Boolean
               val firstColumn = request.params.getParam("columnOne")
               val secondColumn = request.params.getParam("columnTwo")

               if (containsHeader || (!firstColumn.isNullOrEmpty() && !secondColumn.isNullOrEmpty())) {
                  if (!firstColumn.isNullOrEmpty() && !secondColumn.isNullOrEmpty()) {
                     val headerOffset = message.payloadAsText.indexOf("$firstColumn,$secondColumn").orElse(0)

                     if (headerOffset > 0) {
                        message.payload.readPosition(headerOffset)
                     }
                  }
               }

               caskService
                  .ingestRequest(request, Flux.just(message.payload.asInputStream()))
                  .count()
                  .map { "Successfully ingested $it records" }
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
