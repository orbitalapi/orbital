package io.vyne.queryService

import io.netty.handler.timeout.IdleStateEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.socket.server.WebSocketService
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import org.yeauty.annotation.*
import org.yeauty.pojo.Session
//import org.springframework.web.socket.config.annotation.EnableWebSocket
import reactor.core.publisher.Flux

import reactor.core.publisher.Mono

import java.io.IOException


@Configuration
@EnableWebSocket
@Component
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

   class WebFluxWebSocketHandler() : WebSocketHandler {


      override fun handle(webSocketSession: WebSocketSession): Mono<Void> {

         println("Handle session for URI ${webSocketSession.handshakeInfo.getUri().toString()}")

         val stringFlux: Flux<WebSocketMessage> = webSocketSession.receive()
            .map(WebSocketMessage::getPayloadAsText)
            .map(String::toUpperCase)
            .map(webSocketSession::textMessage)
         return webSocketSession.send(stringFlux)
      }
   }

}
