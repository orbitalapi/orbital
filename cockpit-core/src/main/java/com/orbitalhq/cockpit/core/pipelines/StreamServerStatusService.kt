package com.orbitalhq.cockpit.core.pipelines

import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.connections.ConnectionStatus
import com.orbitalhq.http.ServicesConfig.Companion.STREAM_SERVER_NAME
import com.orbitalhq.pipelines.jet.api.streams.StreamServerStatusEvent
import com.orbitalhq.spring.http.websocket.WebSocketController
import com.orbitalhq.spring.rsocket.RSocketConnectionFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks

@RestController
@ConditionalOnProperty("vyne.stream-server.enabled", havingValue = "true", matchIfMissing = false)
class StreamServerStatusService(
   private val discoveryClient: DiscoveryClient,
   private val rSocketConnectionFactory: RSocketConnectionFactory,
   private val objectMapper: ObjectMapper
) : WebSocketController {

   private val sink = Sinks.many().replay().latest<StreamServerStatusWithConnectionMessage>()

   init {
      connectToStreamServerEndpoint()
   }

   private fun connectToStreamServerEndpoint() {
      val (messages, status) = rSocketConnectionFactory.reconnectingRSocket(
         STREAM_SERVER_NAME,
         discoveryClient,
         "stream-server-status",
         Mono.empty(),
         StreamServerStatusEvent::class
      )

      Flux.combineLatest(
         messages.startWith(StreamServerStatusEvent.UKNOWN),
         status
      ) { serverStatusEvent, connectionStatus ->
         val statusIfNotUnknown = if (serverStatusEvent == StreamServerStatusEvent.UKNOWN) null else serverStatusEvent
         StreamServerStatusWithConnectionMessage(connectionStatus, statusIfNotUnknown)
      }
         .subscribe { sink.tryEmitNext(it) }
   }

   override val paths: List<String> = listOf("/api/streams/status")

   override fun handle(session: WebSocketSession): Mono<Void> {
      return session.send(
         sink.asFlux()
            .map { event ->
               session.textMessage(
                  objectMapper.writeValueAsString(event)
               )
            }
      )
   }
}

data class StreamServerStatusWithConnectionMessage(
   val connectionStatus: ConnectionStatus,
   val streamServerState: StreamServerStatusEvent?
)

