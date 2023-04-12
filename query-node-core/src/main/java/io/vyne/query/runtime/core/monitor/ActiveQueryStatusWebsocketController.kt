package io.vyne.query.runtime.core.monitor

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.query.runtime.core.monitor.ActiveQueryMonitor
import io.vyne.spring.http.websocket.WebSocketController
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.asFlux
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

@Component
class ActiveQueryStatusWebsocketController(
    private val activeQueryMonitor: ActiveQueryMonitor,
    private val objectMapper: ObjectMapper
) :
   WebSocketController {

   override val paths = listOf(
      "/api/query/status"
   )

   override fun handle(webSocketSession: WebSocketSession): Mono<Void> {

      val websocketPath = webSocketSession.handshakeInfo.uri.path.toString()

      return when {
         websocketPath == "/api/query/status" -> publishActiveQueryMetadata(webSocketSession)
         else -> webSocketSession.send(emptyFlow<WebSocketMessage>().asFlux())
      }
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
