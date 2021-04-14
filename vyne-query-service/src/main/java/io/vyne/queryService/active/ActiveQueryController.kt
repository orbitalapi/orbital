package io.vyne.queryService.active

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.query.QueryResult
import io.vyne.query.active.ActiveQueryMonitor
import io.vyne.queryService.WebSocketController
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.asFlux
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

@RestController
class ActiveQueryController(private val monitor: ActiveQueryMonitor) {
   @GetMapping("/api/query/active")
   fun liveQueries(): MutableMap<String, QueryResult?> {
      return monitor.runningQueries
   }

   @DeleteMapping("/api/query/active/{id}")
   fun cancelQuery(
      @PathVariable("id") queryId: String
   ) {
      monitor.cancelQuery(queryId)
   }

   @DeleteMapping("/api/query/active/clientId/{id}")
   fun cancelQueryByClientQueryId(
      @PathVariable("id") clientQueryId: String
   ) {
      monitor.cancelQueryByClientQueryId(clientQueryId)
   }

}

@Configuration
class ActiveQueryConfiguration {
   @Bean
   fun activeQueryMonitor() = ActiveQueryMonitor()
}

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
