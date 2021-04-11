package io.vyne.queryService.active

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.query.active.ActiveQueryMonitor
import io.vyne.utils.log
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.asFlux
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.server.WebSocketService
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy
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

   @Bean
   fun handlerAdapter(): WebSocketHandlerAdapter {
      return WebSocketHandlerAdapter(webSocketService())
   }

   fun webSocketService(): WebSocketService {
      return HandshakeWebSocketService(ReactorNettyRequestUpgradeStrategy())
   }


   @Bean
   fun handlerMapping(websocketHandler: WebFluxWebSocketHandler): HandlerMapping {
      val handlerMap: Map<String, WebFluxWebSocketHandler> = mapOf(
         "/api/query/status" to websocketHandler,
         "/api/query/clientId/*/status" to websocketHandler
      )
      return SimpleUrlHandlerMapping(handlerMap, Ordered.HIGHEST_PRECEDENCE)
   }
}

@Component
class WebFluxWebSocketHandler(private val activeQueryMonitor: ActiveQueryMonitor, private val objectMapper:ObjectMapper) : WebSocketHandler {

   private val querySpecificWebsocketPath = "/api/query/clientId/(.+)?/status".toRegex()
   override fun handle(webSocketSession: WebSocketSession): Mono<Void> {

      val websocketPath = webSocketSession.handshakeInfo.uri.path.toString()

      return when {
         websocketPath == "/api/query/status" -> publishActiveQueryMetadata(webSocketSession)
         websocketPath.matches(querySpecificWebsocketPath) -> publishSpecificQueryMetadata(
            websocketPath,
            webSocketSession
         )
         else -> webSocketSession.send(emptyFlow<WebSocketMessage>().asFlux())
      }
   }

   private fun publishSpecificQueryMetadata(
      websocketPath: String,
      webSocketSession: WebSocketSession
   ): Mono<Void> {
      val queryIdMatchResult = querySpecificWebsocketPath.find(websocketPath)
         ?: error("Expected a queryId passed in $websocketPath but one wasn't found")
      val queryId = queryIdMatchResult.groupValues[1]
      log().debug("Attached query results for queryId $queryId to websocket session ${webSocketSession.id}")
      return webSocketSession.send(
         activeQueryMonitor.queryStatusUpdates(queryId)
            .map { runningQueryStatus ->
            val json =   objectMapper.writeValueAsString(runningQueryStatus)
            json
            }
            .map(webSocketSession::textMessage)
            .asFlux()
      )
   }

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
