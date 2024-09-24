package com.orbitalhq.nebula

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.utils.io.core.*
import io.ktor.websocket.*
import io.rsocket.kotlin.RSocket
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

   private var shouldReconnect = true
   fun connect(): Flow<WebSocketSession> = flow {
      while (shouldReconnect) {
         try {
            logger.info { "Attempting to build a new Websocket client for $serviceName" }
            val serviceConfig = discoveryClient.getInstances(serviceName)
               .firstOrNull()
               ?: throw IllegalStateException("No service named $serviceName is present in the config - update your services.conf")

            val urlString = (serviceConfig.uri.toASCIIString() + path).replace("http", "ws")
            val webSocketSession = client.webSocketSession(urlString)
            emit(webSocketSession)
            monitor(webSocketSession)
            // TODO :
            // detect when the websocket session is terminated by the server
            // and reconnect
         } catch (e: Exception) {
            logger.info { "RSocket connection to $serviceName dropped - ${e.message} - Retrying in ${reconnectDelay}" }
            delay(reconnectDelay)
         }
      }
   }.shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.Lazily, replay = 1)

   private suspend fun monitor(webSocketSession: DefaultClientWebSocketSession) {
      while (webSocketSession.isActive) {
         webSocketSession.outgoing.send(Frame.Ping("".toByteArray()))
         delay(30_000)
      }
   }


}
