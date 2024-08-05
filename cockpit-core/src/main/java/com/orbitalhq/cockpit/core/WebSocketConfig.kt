package com.orbitalhq.cockpit.core

import com.orbitalhq.cockpit.core.security.AuthenticationWebSocketHandler
import com.orbitalhq.spring.http.websocket.WebSocketController
import lang.taxi.utils.log
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
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


   /**
    * Exposes the defined websocket endpoints as routes
    * (anything that implements WebSocketController).
    * Also, if a ReactiveAuthenticationManager is present (ie., if auth is enabled),
    * then wires up an auth-specific handler that will inject the auth credentials into
    * the session
    */
   @Bean
   fun handlerMapping(websocketHandlers: List<WebSocketController>, auth: ReactiveAuthenticationManager? = null): HandlerMapping {
      val handlersByPath: Map<String, WebSocketHandler> = websocketHandlers.flatMap { webSocketController ->
         webSocketController.paths.map { path ->
            log().info("Registered websocket handler for $path to ${webSocketController::class.simpleName}")
            val handler = if (auth != null) {
               AuthenticationWebSocketHandler(webSocketController, auth)
            } else {
               webSocketController
            }
            path to handler
         }
      }.toMap()
      return SimpleUrlHandlerMapping(handlersByPath, Ordered.HIGHEST_PRECEDENCE)
   }
}

