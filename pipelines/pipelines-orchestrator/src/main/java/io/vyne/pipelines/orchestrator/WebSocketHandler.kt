package io.vyne.pipelines.orchestrator

import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.AbstractWebSocketHandler


@Component
class WebSocketHandler: AbstractWebSocketHandler() {

   lateinit var session: WebSocketSession

   override fun afterConnectionEstablished(session: WebSocketSession) {
      super.afterConnectionEstablished(session)
   }

   fun sendMessge(message: String) {
      session.sendMessage(TextMessage(message))
   }

}
