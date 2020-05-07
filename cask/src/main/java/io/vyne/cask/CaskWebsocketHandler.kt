package io.vyne.cask

import arrow.core.Either
import io.vyne.schemas.VersionedType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class CaskWebsocketHandler(val caskService: CaskService) : WebSocketHandler {
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
                    val input = Flux.just(websocketMessage.payload.asInputStream())
                    try {
                        val ingestionResult = caskService
                                .ingestRequest(versionedType, input)
                                .count()
                                .map { count ->
                                    // LENS-43 define api that signals successful ingestion
                                    val response = """{"success": true, "message": "Successfully ingested ${count} records"}"""
                                    session.textMessage(response)
                                }
                        session.send(ingestionResult).subscribe()
                    } catch (e: Exception) {
                        log().error("Error ingesting message from sessionId=${session.id}", e)
                        // LENS-43 define api that can signal errors to clients
                        val response = """{"success": false, "message": "Error ingesting message"}"""
                        session.send(Flux.just(session.textMessage(response))).subscribe()
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
