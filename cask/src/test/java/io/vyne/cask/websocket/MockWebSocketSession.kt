package io.vyne.cask.websocket

import org.reactivestreams.Publisher
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.HandshakeInfo
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.publisher.Mono
import java.net.URI
import java.util.*
import java.util.function.Function

class MockWebSocketSession(val uri: String,
                           val sessionId: String = UUID.randomUUID().toString(),
                           val input: Flux<WebSocketMessage> = Flux.empty()) : WebSocketSession {
   var closed: Boolean = false
   lateinit var closeStatus: CloseStatus
   val textOutput: EmitterProcessor<String> = EmitterProcessor.create()
   val textOutputSink: FluxSink<String> = textOutput.sink()

   override fun getId(): String {
      return sessionId
   }

   override fun binaryMessage(payloadFactory: Function<DataBufferFactory, DataBuffer>): WebSocketMessage {
      TODO("Not yet implemented")
   }

   override fun pingMessage(payloadFactory: Function<DataBufferFactory, DataBuffer>): WebSocketMessage {
      TODO("Not yet implemented")
   }

   override fun bufferFactory(): DataBufferFactory {
      TODO("Not yet implemented")
   }

   override fun getAttributes(): MutableMap<String, Any> {
      TODO("Not yet implemented")
   }

   override fun receive(): Flux<WebSocketMessage> {
      return input
   }

   override fun pongMessage(payloadFactory: Function<DataBufferFactory, DataBuffer>): WebSocketMessage {
      TODO("Not yet implemented")
   }

   override fun getHandshakeInfo(): HandshakeInfo {
      return HandshakeInfo(URI(uri), HttpHeaders.EMPTY, Mono.empty(), "ws")
   }

   override fun send(messages: Publisher<WebSocketMessage>): Mono<Void> {
      return Mono.from(messages).doOnNext {
         textOutputSink.next(it.payloadAsText)
      }.then()
   }

   override fun close(status: CloseStatus): Mono<Void> {
      this.closed = true
      this.closeStatus = status
      return Mono.empty()
   }

   override fun textMessage(payload: String): WebSocketMessage {
      return WebSocketMessage(WebSocketMessage.Type.TEXT, MockDataBuffer(payload.byteInputStream()))
   }

}
