package io.vyne.cask.websocket

import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

fun URI.queryParams(): MultiValueMap<String, String?>? {
   return UriComponentsBuilder.fromUri(this).build().queryParams
}

fun WebSocketSession.queryParams(): MultiValueMap<String, String?>? {
   return this.handshakeInfo.uri.queryParams()
}

fun MultiValueMap<String, String?>.getParam(paramName: String): String? {
   return this.get(paramName)?.firstOrNull()
}
