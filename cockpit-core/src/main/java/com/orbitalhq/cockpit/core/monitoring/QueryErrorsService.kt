package com.orbitalhq.cockpit.core.monitoring

import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.connectors.StreamErrorPublisher
import com.orbitalhq.history.db.QueryHistoryRecordRepository
import com.orbitalhq.query.runtime.core.monitor.ActiveQueryMonitor
import com.orbitalhq.spring.http.BadRequestException
import com.orbitalhq.spring.http.websocket.OrbitalWebSocketConfiguration
import com.orbitalhq.spring.http.websocket.WebSocketController
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.util.UriTemplate
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Exposes websocket endpoints for publishing errors about running queries
 */
@RestController
class QueryErrorsService(
   val errorPublisher: StreamErrorPublisher,
   val activeQueryMonitor: ActiveQueryMonitor,
   val objectMapper: ObjectMapper,
   val queryHistoryRecordRepository: QueryHistoryRecordRepository,
   private val orbitalWebSocketConfiguration: OrbitalWebSocketConfiguration
) : WebSocketController {

   override val paths: List<String> = listOf("/api/query/taxiql/{clientQueryId}/errors")
   private val uriTemplate = UriTemplate(paths.single())
   override fun handle(session: WebSocketSession): Mono<Void> {
      val uriVariables = uriTemplate.match(session.handshakeInfo.uri.path)
      val clientQueryId = uriVariables["clientQueryId"]
         ?: throw BadRequestException("Failed to extract the clientQueryId from the provided path ${session.handshakeInfo.uri.path}")

      val querySummary = queryHistoryRecordRepository.findByClientQueryId(clientQueryId)
         ?: throw BadRequestException("No query with clientQueryId of $clientQueryId found. Try again later")

      val outbound = errorPublisher.errors
         .filter { event -> event.queryId == querySummary.queryId }
         .map { event ->
            val json = objectMapper.writeValueAsString(event)
            session.textMessage(json)
         }
      return orbitalWebSocketConfiguration.applyPingConfiguration(this, session, outbound)
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
