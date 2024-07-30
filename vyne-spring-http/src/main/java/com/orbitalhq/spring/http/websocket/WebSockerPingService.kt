package com.orbitalhq.spring.http.websocket

import mu.KotlinLogging
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

private val logger = KotlinLogging.logger {  }
@ConfigurationProperties(prefix = "vyne.ws")
data class OrbitalWebSocketConfiguration(
    val enablePing: Boolean = true,
    val pingIntervalInMilliSeconds: Long = 30000
) {
    fun applyPingConfiguration(socketController: WebSocketController,
                               session: WebSocketSession,
                               outboundStream: Flux<WebSocketMessage>): Mono<Void> {
        return if (enablePing) {
            val income: Flux<WebSocketMessage> = session.receive().share()

            /**
             * As per https://www.rfc-editor.org/rfc/rfc6455#page-37
             *
             * Upon receipt of a Ping frame, an endpoint MUST send a Pong frame in
             * response, unless it already received a Close frame.  It SHOULD
             * respond with Pong frame as soon as is practical
             *
             * Our client side websocket library seems to be sending 'pong' requests automatically against received ping requests.
             */
            val sendPings = session.send(
                Flux.interval(Duration.ofMillis(pingIntervalInMilliSeconds))
                    .map { _  -> session.pingMessage {
                            dataBufferFactory -> dataBufferFactory.allocateBuffer() }
                    })

            val pong: Flux<WebSocketMessage> = income
                .filter { m -> m.type == WebSocketMessage.Type.PONG }
                .doOnNext { _ ->
                    logger.trace { "pong => ${session.id}, from paths => ${socketController.paths}" }
                }
            val outbound = session.send(outboundStream)
            Flux.merge(sendPings, outbound, pong).then()
        } else {
            session.send(outboundStream)
        }
    }
}


@Configuration
@EnableConfigurationProperties(OrbitalWebSocketConfiguration::class)
class WebSocketPingConfig