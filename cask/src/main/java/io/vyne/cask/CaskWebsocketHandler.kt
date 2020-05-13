package io.vyne.cask

import arrow.core.Either
import com.fasterxml.jackson.core.io.JsonEOFException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.cask.api.CaskIngestionResponse
import io.vyne.cask.websocket.getParam
import io.vyne.cask.websocket.queryParams
import io.vyne.schemas.VersionedType
import io.vyne.utils.log
import io.vyne.utils.orElse
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.CloseStatus.NOT_ACCEPTABLE
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class CaskWebsocketHandler(val caskService: CaskService, val mapper: ObjectMapper = jacksonObjectMapper()) : WebSocketHandler {

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
      return session.receive()
         .doOnNext { websocketMessage ->
            ingestMessage(
               session,
               websocketMessage,
               versionedType,
               contentType,
               sendResponse)
         }
         .doOnComplete {
            log().info("Closing sessionId=${session.id}")
         }
         .doOnError { error ->
            log().error("Error ingesting message from sessionId=${session.id}", error)
         }
         .then()
   }

   private fun ingestMessage(session: WebSocketSession,
                             websocketMessage: WebSocketMessage,
                             versionedType: VersionedType,
                             contentType: MediaType,
                             sendResponse: Boolean) {

      log().info("Ingesting message from sessionId=${session.id}")
      try {
         val input = Flux.just(websocketMessage.payload.asInputStream())
         val ingestionResult = caskService
            .ingestRequest(versionedType, input, contentType)
            .count()
            .filter { sendResponse }
            .map { "Successfully ingested ${it} records" }
            .map { CaskIngestionResponse.success(it) }
            .map(mapper::writeValueAsString)
            .map(session::textMessage)
         session.send(ingestionResult).subscribe()
      } catch (e: Exception) {
         log().error("Error ingesting message from sessionId=${session.id}", e)
         when (e.cause) {
            // This can leak some of the internal data structures/classes
            is IllegalArgumentException -> respondWithError(session, e.message.orElse("An IllegalArgumentException was thrown, but no further details are available."))
            is JsonEOFException -> respondWithError(session, "Malformed JSON message")
            else -> respondWithError(session, "Unexpected ingestion error")
         }
      }
   }

   private fun respondWithError(
      session: WebSocketSession,
      errorMessage: String) {
      val errorResult = Flux.just(errorMessage)
         .map { CaskIngestionResponse.rejected(it) }
         .map(mapper::writeValueAsString)
         .map(session::textMessage)
      session.send(errorResult).subscribe()
   }
}
