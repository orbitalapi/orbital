package io.vyne.cask.websocket

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrHandle
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import io.vyne.cask.CaskIngestionRequest
import io.vyne.cask.CaskService
import io.vyne.cask.api.CaskIngestionResponse
import io.vyne.cask.api.ContentType
import io.vyne.cask.ingest.CaskIngestionErrorProcessor
import io.vyne.cask.ingest.IngestionError
import io.vyne.cask.ingest.IngestionInitialisedEvent
import io.vyne.utils.log
import io.vyne.utils.orElse
import org.postgresql.util.PSQLException
import org.postgresql.util.PSQLState
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
import java.util.*

@Component
class CaskWebsocketHandler(
   val caskService: CaskService,
   val applicationEventPublisher: ApplicationEventPublisher,
   val caskIngestionErrorProcessor: CaskIngestionErrorProcessor,
   @Qualifier("ingesterMapper") val mapper: ObjectMapper) : WebSocketHandler {

   override fun handle(session: WebSocketSession): Mono<Void> {
      log().info("Opening new sessionId=${session.id} uri=${session.handshakeInfo.uri} remoteAddress=${session.handshakeInfo.remoteAddress}")

      val requestOrError = requestOrError(session)

      return requestOrError
         .map { request ->
            applicationEventPublisher.publishEvent(IngestionInitialisedEvent(this, request.versionedType))
            ingestMessages(session, request)
         }.getOrHandle { error ->
            log().info("Closing sessionId=${session.id}.  Error: ${error.message}")
            session.close(CloseStatus(NOT_ACCEPTABLE.code, error.message)).then()
         }
   }

   private fun requestOrError(session: WebSocketSession): Either<CaskService.CaskServiceError, CaskIngestionRequest> {
     return try {
         Either.right(ContentType.valueOf(session.contentType()))
      } catch (exception:IllegalArgumentException) {
         Either.left(CaskService.ContentTypeError("Unknown contentType=${session.contentType()}"))
      }.flatMap { contentType ->
         caskService.resolveType(session.typeReference()).map { versionedType ->
            CaskIngestionRequest.fromContentTypeAndHeaders(contentType, versionedType, mapper, session.queryParams(), caskIngestionErrorProcessor)
         }
      }
   }

   private fun ingestMessages(session: WebSocketSession, request: CaskIngestionRequest): Mono<Void> {
      val output: EmitterProcessor<WebSocketMessage> = EmitterProcessor.create()
      val outputSink = output.sink()
      var currentRequest = request
      // i don't like this, it's pretty ugly
      // partially because exceptions are thrown outside flux pipelines
      // we have to refactor code behind ingestion to fix this problem
//      session.receive()
//         .name("cask_ingestion_request")
//         // This will register timer with the above name
//         // Registry instance is auto-detected
//         // Percentiles are configured globally for all the timers, see CaskApp
//         .metrics()
//         .map { message ->
//            val messageId = UUID.randomUUID().toString()
//            try {
//               caskService
//                  .ingestRequest(currentRequest, message.payload.asInputStream(), messageId)
//
//                  .count()
//                  .map { "Successfully ingested $it records" }
//                  .subscribe(
//                     { result ->
////                        log().info("$result from sessionId=${session.id}")
//                        if (request.debug) {
//                           outputSink.next(successResponse(session, result))
//                        }
//                     },
//                     { error ->
//                        log().error("Ws Handler Error ingesting message from sessionId=${session.id}", error)
//                        if (error is PSQLException && error.sqlState == PSQLState.UNDEFINED_TABLE.state) {
//                           // Table not found - this should be due to schema change.
//                           // update CaskIngestionRequest with the new schema info and re-try.
//                           requestOrError(session).map {
//                              currentRequest = it
//                              reIngestRequest(currentRequest, message).block() // blocking is not nice, but this should happen very rare.
//                           }
//                        }
//                        outputSink.next(errorResponse(session, extractError(error)))
//                        caskIngestionErrorProcessor.sink().next(IngestionError.fromThrowable(error, messageId, request.versionedType))
//                     }
//                  )
//            } catch (error: Exception) {
//               log().error("Ws Handler Error ingesting message from sessionId=${session.id}", error)
//               outputSink.next(errorResponse(session, extractError(error)))
//               caskIngestionErrorProcessor.sink().next(IngestionError.fromThrowable(error, messageId, request.versionedType))
//            }
//         }
//         .doOnComplete {
//            log().info("Closing sessionId=${session.id}")
//            output.onComplete()
//         }
//         .doOnError { error ->
//            log().error("Error ingesting message from sessionId=${session.id}", error)
//         }
//         .subscribe()
//
//      return session.send(output)
      TODO()
   }

   private fun reIngestRequest(request: CaskIngestionRequest, message: WebSocketMessage): Mono<CaskIngestionResponse> {
//      return caskService.ingestRequest(request, message.payload.asInputStream())
//         .count()
//         .map { CaskIngestionResponse.success("Successfully ingested $it records") }
//         .onErrorResume {
//            log().error("Ingestion error", it)
//            Mono.just(CaskIngestionResponse.rejected(it.toString()))
//         }
      TODO()
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
