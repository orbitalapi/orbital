package com.orbitalhq.nebula

import com.orbitalhq.connections.ConnectionStatus
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.utils.io.core.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import mu.KotlinLogging
import org.springframework.cloud.client.discovery.DiscoveryClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration

class ReconnectingKtorWebsocket(
   private val discoveryClient: DiscoveryClient,
   private val serviceName: String,
   private val reconnectDelay: Duration,
   private val path: String = ""
) {
   val client = HttpClient(CIO) {
      install(WebSockets)
   }

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   private val statusSink = Sinks.many().multicast().onBackpressureBuffer<ConnectionStatus>()
   val status: Flux<ConnectionStatus> = statusSink.asFlux()
      .replay(1)
      .autoConnect()

   init {
      statusSink.tryEmitNext(ConnectionStatus.notStarted())
   }

   private var shouldReconnect = true
   private var urlString = "Unknown url"
   private val connected = AtomicBoolean(false)
   fun connect(): Flow<WebSocketSession> = flow {
      while (shouldReconnect) {
         try {
            logger.debug { "Attempting to build a new Websocket client for $serviceName" }
            val serviceConfig = discoveryClient.getInstances(serviceName)
               .firstOrNull()
               ?: run {
                  val message = "No service named $serviceName is present in the config - update your services.conf"
                  statusSink.tryEmitNext(ConnectionStatus.error(message))
                  throw IllegalStateException(message)
               }

            urlString = (serviceConfig.uri.toASCIIString() + path).replace("http", "ws")
            logger.debug { "Attempting to connect to nebula at $urlString" }
            statusSink.tryEmitNext(
               ConnectionStatus(
                  ConnectionStatus.Status.CONNECTING,
                  "Attempting to connect to $urlString"
               )
            )
            val webSocketSession = client.webSocketSession(urlString)
            emit(webSocketSession)

            connected.set(true)
            logger.info { "Websocket for $serviceName connected" }
            statusSink.tryEmitNext(ConnectionStatus.healthy("Connected to $urlString"))

            // This blocks the current thread until the session dies
            blockAndSendPingMessages(webSocketSession)

            // TODO :
            // detect when the websocket session is terminated by the server
            // and reconnect
         } catch (e: Exception) {
            val wasConnected = connected.getAndSet(false)
            if (wasConnected) {
               logger.info { "Websocket connection to $serviceName dropped - ${e.message} - Retrying in ${reconnectDelay}" }
            } else {
               logger.debug { "Nebula connection failed: ${e::class.simpleName} - ${e.message}" }
            }
            statusSink.tryEmitNext(ConnectionStatus.error("Disconnected from $urlString - ${e::class.simpleName} - ${e.message}"))
            delay(reconnectDelay)
         }
      }
   }.shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.Lazily, replay = 1)

   private suspend fun blockAndSendPingMessages(webSocketSession: DefaultClientWebSocketSession) {
      while (webSocketSession.isActive) {
         webSocketSession.outgoing.send(Frame.Ping("".toByteArray()))
         delay(30_000)
      }

   }


}
