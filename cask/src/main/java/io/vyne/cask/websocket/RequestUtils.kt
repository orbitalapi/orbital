package io.vyne.cask.websocket

import io.vyne.utils.orElse
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

fun URI.queryParams(): MultiValueMap<String, String?>? {
   return UriComponentsBuilder.newInstance().query(this.query).build().queryParams
}

fun WebSocketSession.queryParams(): MultiValueMap<String, String?>? {
   return this.handshakeInfo.uri.queryParams()
}

fun MultiValueMap<String, String?>.getParam(paramName: String): String? {
   return this.get(paramName)?.firstOrNull()
}
fun MultiValueMap<String, String?>.getParams(paramName: String): MutableList<String?>? {
   return this.get(paramName)
}

fun WebSocketSession.typeReference() : String {
   // backward-compatibility with pipeline-runner, e.x. uri=/cask/[typeReference]
   // TODO remove this once we migrate to new uri structure /cask/[contentType]/[typeReference]
   return this.handshakeInfo.uri.path.replace("""/cask/(\w+/)?""".toRegex(), "")
}

fun WebSocketSession.contentType(): String {
   val regex = """/cask/(\w+)/.*""".toRegex()
   val path = handshakeInfo.uri.path
   return regex.find(path)
      ?.groupValues?.get(1)
      .orElse("json")
}
