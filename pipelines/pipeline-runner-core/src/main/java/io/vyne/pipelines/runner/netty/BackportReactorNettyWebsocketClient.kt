package io.vyne.pipelines.runner.netty

import org.springframework.core.io.buffer.NettyDataBufferFactory
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.socket.HandshakeInfo
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.adapter.ReactorNettyWebSocketSession
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Mono
import reactor.netty.http.websocket.WebsocketInbound
import reactor.netty.http.websocket.WebsocketOutbound
import java.net.URI
import java.util.function.Consumer

/**
 * This class is copied from reactor-netty 0.9.5, and exposes methods backwards compatible methods
 * between 0.9.5 and reactor-netty-http 1.0.5
 */
class BackportReactorNettyWebsocketClient : ReactorNettyWebSocketClient() {
   override fun execute(url: URI, requestHeaders: HttpHeaders?, handler: WebSocketHandler): Mono<Void?>? {
      return httpClient
         .headers { nettyHeaders: io.netty.handler.codec.http.HttpHeaders? ->
            setNettyHeaders(
               requestHeaders,
               nettyHeaders
            )
         }
         .websocket()
         .uri(url.toString())
         .handle { inbound: WebsocketInbound?, outbound: WebsocketOutbound ->
            val responseHeaders = toHttpHeaders(inbound)
            val protocol = responseHeaders?.getFirst("Sec-WebSocket-Protocol")
            val info = HandshakeInfo(url, responseHeaders, Mono.empty(), protocol)
            val factory = NettyDataBufferFactory(outbound.alloc())
            val session: WebSocketSession = ReactorNettyWebSocketSession(
               inbound, outbound, info, factory, maxFramePayloadLength
            )
            handler.handle(session).checkpoint("$url [ReactorNettyWebSocketClient]")
         }
         .next()
   }
   private fun setNettyHeaders(httpHeaders: HttpHeaders?, nettyHeaders: io.netty.handler.codec.http.HttpHeaders?) {
      httpHeaders?.forEach { s: String?, iterable: List<String?>? ->
         nettyHeaders?.set(s, iterable)
      }
   }
   private fun toHttpHeaders(inbound: WebsocketInbound?): HttpHeaders? {
      val headers = HttpHeaders()
      val nettyHeaders = inbound?.headers()
      nettyHeaders?.forEach(Consumer { entry: Map.Entry<String, String?> ->
         val name = entry.key
         headers[name] = nettyHeaders.getAll(name)
      })
      return headers
   }
}
