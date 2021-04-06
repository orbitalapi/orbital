package io.vyne.queryService

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.utils.log
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.asFlux
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.server.WebSocketService
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

@Configuration
@Component
@EnableScheduling
class QueryMetaDataController {

   @Bean
   fun handlerAdapter(): WebSocketHandlerAdapter {
      return WebSocketHandlerAdapter(webSocketService())
   }

   fun webSocketService(): WebSocketService {
      return HandshakeWebSocketService(ReactorNettyRequestUpgradeStrategy())
   }

   @Bean
   fun handlerMapping(): HandlerMapping {
      println("Handler mapping ")
      val handlerMap: Map<String, WebFluxWebSocketHandler> = mapOf(
         "/api/vyneql/metadata" to WebFluxWebSocketHandler(),
         "/api/vyneql/*/metadata" to WebFluxWebSocketHandler()
      )
      return SimpleUrlHandlerMapping(handlerMap, Ordered.HIGHEST_PRECEDENCE)
   }
}

class WebFluxWebSocketHandler() : WebSocketHandler {

   val objectMapper: ObjectMapper = ObjectMapper()

   override fun handle(webSocketSession: WebSocketSession): Mono<Void> {

      if ("/api/vyneql/metadata".equals(webSocketSession.handshakeInfo.uri.path.toString())) {

         return webSocketSession.send(QueryMetaDataService.monitor.metaDataEvents()
            ?.map{objectMapper.writeValueAsString(it)}
            ?.map(webSocketSession::textMessage)
            ?.asFlux()
         )

      } else if (webSocketSession.handshakeInfo.uri.path.toString().matches("""/api/vyneql/(.+)?/metadata""".toRegex())) {

         val regex = """/api/vyneql/(.+)?/metadata""".toRegex()
         val matchResult = regex.find(webSocketSession.handshakeInfo.uri.path.toString())
         log().debug("Attached query results for queryId ${matchResult?.groupValues?.get(1)} to websocket session ${webSocketSession.id}")
         return webSocketSession.send(
            QueryMetaDataService.monitor.queryMetaDataEvents(matchResult?.groupValues?.get(1)!!)
               ?.map{objectMapper.writeValueAsString(it)}
               ?.map(webSocketSession::textMessage)
               ?.asFlux()
         )

      } else {

         return webSocketSession.send(emptyFlow<WebSocketMessage>().asFlux())

      }

   }
}
