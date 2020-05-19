package io.vyne.cask

import arrow.core.Either
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.cask.api.CaskIngestionResponse
import io.vyne.cask.ingest.IngestionInitialisedEvent
import io.vyne.cask.websocket.getParam
import io.vyne.cask.websocket.queryParams
import io.vyne.schemas.VersionedType
import io.vyne.utils.log
import io.vyne.utils.orElse
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.MediaType
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
      log().info("Opening new sessionId=${session.id} uri=${session.handshakeInfo.uri.path} query=${session.handshakeInfo.uri.query}")

      val queryParams = session.queryParams()
      val contentTypeName = queryParams?.getParam("contentType").orElse(MediaType.APPLICATION_JSON_VALUE)

      when (val contentType = caskService.resolveContentType(contentTypeName)) {
         is Either.Left -> {
            log().info("Closing sessionId=${session.id}.  Error: ${contentType.a.message}")
            return session
               .close(CloseStatus(NOT_ACCEPTABLE.code, contentType.a.message))
               .then()
         }
         is Either.Right -> {
            val sendResponse = queryParams
               ?.getParam("debug")
               .orElse("false")
               .equals("true")

            return findTypeReference(
               session,
               contentType.b,
               sendResponse)
         }
      }
   }

   private fun findTypeReference(session: WebSocketSession,
                                 contentType: MediaType,
                                 sendResponse: Boolean): Mono<Void> {

      val typeReferenceFromPath = session.handshakeInfo.uri.path.replace("/cask/", "")
      when (val versionedType = caskService.resolveType(typeReferenceFromPath)) {
         is Either.Left -> {
            log().info("Closing sessionId=${session.id}. Error: ${versionedType.a.message}")
            return session
               .close(CloseStatus(NOT_ACCEPTABLE.code, versionedType.a.message))
               .then()
         }
         is Either.Right -> {
            applicationEventPublisher.publishEvent(IngestionInitialisedEvent(this, versionedType.b))
            return ingestMessages(
               session,
               versionedType.b,
               contentType,
               sendResponse)
         }
      }
   }

   private fun ingestMessages(session: WebSocketSession,
                              versionedType: VersionedType,
                              contentType: MediaType,
                              sendResponse: Boolean): Mono<Void> {
      val output: EmitterProcessor<WebSocketMessage> = EmitterProcessor.create()
      val outputSink = output.sink()

      // i don't like this, it's pretty ugly
      // partially because exceptions are thrown outside flux pipelines
      // we have to refactor code behind ingestion to fix this problem
      session.receive()
         .map {
            log().info("Ingesting message from sessionId=${session.id}")
            try {
               caskService
                  .ingestRequest(versionedType, Flux.just(it.payload.asInputStream()), contentType)
                  .count()
                  .map { "Successfully ingested ${it} records" }
                  .subscribe(
                     { result ->
                        if (sendResponse) {
                           outputSink.next(successResponse(session, result))
                        }
                     },
                     { error ->
                        log().error("Error ingesting message from sessionId=${session.id} ", error)
                        outputSink.next(errorResponse(session, extractError(error)))
                     }
                  )
            } catch (error: Exception) {
               log().error("Error ingesting message from sessionId=${session.id} ", error)
               outputSink.next(errorResponse(session, extractError(error)))
            }
         }
         .doOnComplete {
            log().info("Closing sessionId=${session.id}")
            output.onComplete()
         }
         .doOnError { error ->
            log().error("Error ingesting message from sessionId=${session.id} ", error)
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
