package com.orbitalhq.cockpit.core.pipelines

import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.query.runtime.StreamResultStreamProvider
import com.orbitalhq.auth.EmptyAuthenticationToken
import com.orbitalhq.query.runtime.core.gateway.WebsocketQueryRouteMatchingService
import com.orbitalhq.spring.http.NotFoundException
import com.orbitalhq.spring.http.websocket.OrbitalWebSocketConfiguration
import com.orbitalhq.spring.http.websocket.WebSocketController
import mu.KotlinLogging
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Responsible for publishing results of saved query streams
 * over a websocket.
 *
 * Subscribes to an event stream from the stream server using RSocket,
 * then publishes these results over a websocket.
 */
@RestController
class StreamResultsWebsocketPublisher(
   private val streamResultSubscriptionManager: StreamResultStreamProvider,
   private val routeMatchingService: WebsocketQueryRouteMatchingService,
   private val objectMapper: ObjectMapper,
   private val orbitalWebSocketConfiguration: OrbitalWebSocketConfiguration
) : WebSocketController {
   companion object {
      val PATH_PREFIX = "/api/s/"
      val PATH = "$PATH_PREFIX**"

      private val logger = KotlinLogging.logger {}
   }

   override val paths: List<String>
      get() = listOf(PATH)

   override fun handle(session: WebSocketSession): Mono<Void> {
      val queryPath = session.handshakeInfo.uri
         .path

      val notFound = Mono.error<Void>(NotFoundException("No query defined at path $queryPath"))

      // Security: We only accept websockets published at a predefined prefix.
      // This is to prevent someone publishing a query at an internal or dangerous
      // path
      if (!queryPath.startsWith(PATH_PREFIX)) {
         return notFound
      }
      val query = routeMatchingService.getQuery(queryPath)
         ?: return notFound

      val outbound = session.handshakeInfo.principal.defaultIfEmpty(EmptyAuthenticationToken)
         .flatMapMany { principalOrEmpty ->
            val principal = if (principalOrEmpty is EmptyAuthenticationToken) null else principalOrEmpty
            val flux: Flux<Any> = streamResultSubscriptionManager.getResultStream(query.name.parameterizedName,principal)
            flux.map { event ->
               val json = objectMapper.writeValueAsString(event)
               session.textMessage(json)
            }
         }


      return orbitalWebSocketConfiguration.applyPingConfiguration(this, session, outbound)
   }
}
