package io.vyne.cask

import arrow.core.Either
import com.fasterxml.jackson.core.io.JsonEOFException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.cask.api.CaskIngestionResponse
import io.vyne.cask.websocket.queryParams
import io.vyne.schemas.VersionedType
import io.vyne.utils.orElse
import io.vyne.utils.log
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class CaskWebsocketHandler(val caskService: CaskService, val mapper: ObjectMapper = jacksonObjectMapper()) : WebSocketHandler {
    override fun handle(session: WebSocketSession): Mono<Void> {
        val typeReferenceFromPath = session.handshakeInfo.uri.path.replace("/cask/", "")

        log().info("Opening new sessionId=${session.id} uri=${session.handshakeInfo.uri.path} query=${session.handshakeInfo.uri.query}")

       return when (val versionedType = caskService.resolveType(typeReferenceFromPath)) {
            is Either.Left -> {
                log().info("Closing sessionId=${session.id} as the type failed to resolve.  Error: ${versionedType.a.message}")
                /**
                 * 1003 indicates that an endpoint is terminating the connection
                 * because it has received a type of data it cannot accept.
                 * Reference: https://tools.ietf.org/html/rfc6455#section-7.4
                 */
                val closeStatus = CloseStatus(CloseStatus.NOT_ACCEPTABLE.code, versionedType.a.message)
                return session.close(closeStatus).then()
            }
            is Either.Right -> {
                return ingestMessages(session, versionedType.b)
            }

        }
    }

    private fun ingestMessages(session: WebSocketSession, versionedType: VersionedType): Mono<Void> {
       // ReactorNettyRequestUpgradeStrategy.upgrade method does not pass request object so we have to parse uri.query
       val queryParams = session.handshakeInfo.uri.queryParams()
       val sendIngestionResponse = queryParams?.get("debug")?.firstOrNull().orElse("false").equals("true")
        return session.receive()
                .doOnNext { websocketMessage ->
                    log().info("Ingesting message from sessionId=${session.id}")
                    try {
                        val input = Flux.just(websocketMessage.payload.asInputStream())
                        val ingestionResult = caskService
                                .ingestRequest(versionedType, input)
                                .count()
                                .filter { sendIngestionResponse }
                                .map { "Successfully ingested ${it} records" }
                                .map { CaskIngestionResponse.success(it) }
                                .map(mapper::writeValueAsString)
                                .map(session::textMessage)
                        session.send(ingestionResult).subscribe()
                    } catch (e: Exception) {
                       log().error("Error ingesting message from sessionId=${session.id}", e)
                       when(e.cause) {
                          // This can leak some of the internal data structures/classes
                          is IllegalArgumentException -> respondWithError(session, e.message.orElse("An IllegalArgumentException was thrown, but no further details are available."))
                          is JsonEOFException -> respondWithError(session, "Malformed JSON message")
                          else -> respondWithError(session, "Unexpected ingestion error")
                       }

                    }
                }
                .doOnComplete {
                    log().info("Closing sessionId=${session.id}")
                }
                .doOnError { error ->
                    log().error("Error ingesting message from sessionId=${session.id}", error)
                }
                .then()
    }

   private fun respondWithError(session: WebSocketSession, errorMessage: String) {
      val errorResult = Flux.just(errorMessage)
         .map { CaskIngestionResponse.rejected(it) }
         .map(mapper::writeValueAsString)
         .map(session::textMessage)
      session.send(errorResult).subscribe()
   }
}
