package io.vyne.cask

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.cask.api.CaskIngestionResponse
import io.vyne.schemas.VersionedType
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

        log().info("Opening new sessionId=${session.id} uri=${session.handshakeInfo.uri.path}")

        val versionedType = caskService.resolveType(typeReferenceFromPath)
        return when (versionedType) {
            is Either.Right -> {
                log().info("Closing sessionId=${session.id} as the type failed to resolve.  Error: ${versionedType.b.message}")
                /**
                 * 1003 indicates that an endpoint is terminating the connection
                 * because it has received a type of data it cannot accept.
                 * Reference: https://tools.ietf.org/html/rfc6455#section-7.4
                 */
                val closeStatus = CloseStatus(CloseStatus.NOT_ACCEPTABLE.code, versionedType.b.message)
                return session.close(closeStatus).then()
            }
            is Either.Left -> {
                return ingestMessages(session, versionedType.a)
            }

        }
    }

    private fun ingestMessages(session: WebSocketSession, versionedType: VersionedType): Mono<Void> {
        return session.receive()
                .doOnNext { websocketMessage ->
                    log().info("Ingesting message from sessionId=${session.id}")
                    try {
                        val input = Flux.just(websocketMessage.payload.asInputStream())
                        val ingestionResult = caskService
                                .ingestRequest(versionedType, input)
                                .count()
                                .map { "Successfully ingested ${it} records" }
                                .map { CaskIngestionResponse.success(it) }
                                .map(mapper::writeValueAsString)
                                .map(session::textMessage)
                        session.send(ingestionResult).subscribe()
                    } catch (e: Exception) {
                        log().error("Error ingesting message from sessionId=${session.id}", e)
                        val errorResult = Flux.just("Error ingesting message")
                                .map{CaskIngestionResponse.rejected(it)}
                                .map(mapper::writeValueAsString)
                                .map(session::textMessage)
                        session.send(errorResult).subscribe()
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
}
