package io.vyne.queryService.active

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.query.active.ActiveQueryMonitor
import io.vyne.queryService.WebSocketController
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.asFlux
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

@RestController
class ActiveQueryController(private val monitor: ActiveQueryMonitor) {
   @GetMapping("/api/query/active")
   fun liveQueries(): Map<String, String> {
      return monitor.queryIdToClientQueryIdMap
   }

}

@Configuration
class ActiveQueryConfiguration {
   @Bean
   fun activeQueryMonitor() = ActiveQueryMonitor()
}

@Component
class ActiveQueryStatusWebsocketController(private val activeQueryMonitor: ActiveQueryMonitor, private val objectMapper:ObjectMapper) :
   WebSocketController {

   override val paths = listOf(
      "/api/query/status"
   )

   private val querySpecificWebsocketPath = "/api/query/clientId/(.+)?/status".toRegex()
   override fun handle(webSocketSession: WebSocketSession): Mono<Void> {

      val websocketPath = webSocketSession.handshakeInfo.uri.path.toString()

      return when {
         websocketPath == "/api/query/status" -> publishActiveQueryMetadata(webSocketSession)
//         websocketPath.matches(querySpecificWebsocketPath) -> publishSpecificQueryMetadata(
//            websocketPath,
//            webSocketSession
//         )
         else -> webSocketSession.send(emptyFlow<WebSocketMessage>().asFlux())
      }
   }

//   private fun publishSpecificQueryMetadata(
//      websocketPath: String,
//      webSocketSession: WebSocketSession
//   ): Mono<Void> {
//      val queryIdMatchResult = querySpecificWebsocketPath.find(websocketPath)
//         ?: error("Expected a queryId passed in $websocketPath but one wasn't found")
//      val queryId = queryIdMatchResult.groupValues[1]
//      log().debug("Attached query results for queryId $queryId to websocket session ${webSocketSession.id}")
//      return webSocketSession.send(
//         activeQueryMonitor.queryStatusUpdates(queryId)
//            .map { runningQueryStatus ->
//            val json =   objectMapper.writeValueAsString(runningQueryStatus)
//            json
//            }
//            .map(webSocketSession::textMessage)
//            .asFlux()
//      )
//   }

   private fun publishActiveQueryMetadata(webSocketSession: WebSocketSession): Mono<Void> {
      return webSocketSession.send(activeQueryMonitor.allQueryStatusUpdates()
         .map { runningQueryStatus ->
            val json = objectMapper.writeValueAsString(runningQueryStatus)
            json
         }
         .map(webSocketSession::textMessage)
         .asFlux()
      )
   }
}
