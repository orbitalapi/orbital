package com.orbitalhq.cockpit.core.monitoring

import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.query.runtime.core.monitor.ActiveQueryMonitor
import com.orbitalhq.security.VynePrivileges
import com.orbitalhq.spring.http.BadRequestException
import com.orbitalhq.spring.http.websocket.OrbitalWebSocketConfiguration
import com.orbitalhq.spring.http.websocket.WebSocketController
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.util.UriTemplate
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * Exposes websocket endpoints for publishing errors about running queries
 */
@RestController
class QueryErrorsService(
   val activeQueryMonitor: ActiveQueryMonitor,
   val objectMapper: ObjectMapper,
   private val orbitalWebSocketConfiguration: OrbitalWebSocketConfiguration
) : WebSocketController {

   override val paths: List<String> = listOf("/api/query/taxiql/{clientQueryId}/errors")

   private val uriTemplate = UriTemplate(paths.single())

   @PreAuthorize("hasAuthority('${VynePrivileges.RunQuery}')")
   override fun handle(session: WebSocketSession): Mono<Void> {
      val uriVariables = uriTemplate.match(session.handshakeInfo.uri.path)
      val clientQueryId = uriVariables["clientQueryId"]
         ?: throw BadRequestException("Failed to extract the clientQueryId from the provided path ${session.handshakeInfo.uri.path}")

      val queryIdMono = Mono.defer {
         // A race condition exists where we're frequently called on this thread before the
         // query is present on the query monitor.
         // So wait / retry for a bit
         Mono.justOrEmpty(activeQueryMonitor.queryIdFromClientId(clientQueryId))
      }
         .repeatWhenEmpty { it.delayElements(Duration.ofMillis(250)).take(40) } // Retry every 250ms up to 40 times
         .switchIfEmpty(Mono.error(BadRequestException("No query with clientQueryId of $clientQueryId found. Try again later")))

      return queryIdMono.flatMap { queryId ->
         val outbound = activeQueryMonitor.queryErrorPublisherForQueryId(queryId!!)
            ?.queryErrorPublisher
            ?.errors
            ?.map { event ->
               val json = objectMapper.writeValueAsString(event)
               session.textMessage(json)
            } ?: Flux.empty()
          orbitalWebSocketConfiguration.applyPingConfiguration(this, session, outbound)
      }
   }
}

/**
 * Similar to combineLatest - in that it emits messages from both fluxes as they arrive.
 * However, does not wait
 */
fun <A, B> combineLatestWithNulls(fluxA: Flux<A>, fluxB: Flux<B>): Flux<Pair<A?, B?>> {
   // Start each flux with null to ensure combineLatest emits pairs immediately
   val startWithNullA = fluxA.startWith(Flux.just(null))
   val startWithNullB = fluxB.startWith(Flux.just(null))

   // Use combineLatest to combine the two fluxes
   return Flux.combineLatest(
      startWithNullA,
      startWithNullB
   ) { a, b -> Pair(a as? A, b as? B) } // Cast is safe because of startWith(null)

}
