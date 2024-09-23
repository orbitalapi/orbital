package com.orbitalhq.nebula

import com.orbitalhq.http.ServicesConfig
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.jackson.*
import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.ktor.client.RSocketSupport
import io.rsocket.kotlin.ktor.client.rSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import mu.KotlinLogging
import org.springframework.cloud.client.discovery.DiscoveryClient
import kotlin.time.Duration

class ReconnectingKtorRSocket(
   private val discoveryClient: DiscoveryClient,
   private val serviceName: String,
   private val reconnectDelay: Duration,
   private val path: String = ""
) {
   val client = HttpClient(CIO) {
      install(WebSockets)
      install(RSocketSupport)
   }

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   private var shouldReconnect = true
   fun connect(): Flow<RSocket> = flow {
      while (shouldReconnect) {
         try {
            val serviceConfig = discoveryClient.getInstances(serviceName)
               .firstOrNull() ?: throw IllegalStateException("No service named $serviceName is present in the config - update your services.conf")

            val urlString = (serviceConfig.uri.toASCIIString() + path).replace("http", "ws")
            val rsocket = client.rSocket(urlString)

            emit(rsocket)

         } catch (e:Exception) {
            logger.info { "RSocket connection to $serviceName dropped - ${e.message} - attempting to reconnect" }
            delay(reconnectDelay)
         }
      }
   }.shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.Lazily, replay = 1)
}
