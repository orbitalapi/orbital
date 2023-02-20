package io.vyne.cockpit.core

import io.vyne.spring.http.websocket.WebSocketController
import lang.taxi.packages.utils.log
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.server.WebSocketService
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy

@Configuration
class WebSocketConfig {
   @Bean
   fun handlerAdapter(): WebSocketHandlerAdapter {
      return WebSocketHandlerAdapter(webSocketService())
   }

   fun webSocketService(): WebSocketService {
      return HandshakeWebSocketService(ReactorNettyRequestUpgradeStrategy())
   }


   @Bean
   fun handlerMapping(websocketHandlers: List<WebSocketController>): HandlerMapping {
      val handlersByPath: Map<String, WebSocketController> = websocketHandlers.flatMap {
         it.paths.map { path ->
            log().info("Registered websocket handler for $path to ${it::class.simpleName}")
            path to it
         }
      }.toMap()
      return SimpleUrlHandlerMapping(handlersByPath, Ordered.HIGHEST_PRECEDENCE)
   }
}
