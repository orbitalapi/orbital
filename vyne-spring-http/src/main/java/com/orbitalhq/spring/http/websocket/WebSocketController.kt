package com.orbitalhq.spring.http.websocket

import org.springframework.web.reactive.socket.WebSocketHandler

interface WebSocketController : WebSocketHandler {
   val paths: List<String>
}
