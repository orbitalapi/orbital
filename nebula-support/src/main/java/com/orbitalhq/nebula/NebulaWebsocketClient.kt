package com.orbitalhq.nebula

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.orbitalhq.http.ServicesConfig
import com.orbitalhq.nebula.core.StackStateEvent
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import io.rsocket.kotlin.payload.Payload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import kotlin.math.log
import kotlin.time.Duration.Companion.milliseconds

@Component
class NebulaWebsocketClient(
   private val schemaWatcher: NebulaSchemaWatcher,
   private val discoveryClient: DiscoveryClient,
   private val envVariableSource: NebulaEnvVariableSource,
   private val objectMapper: ObjectMapper,
   private val nebulaSubmissionService: NebulaSubmissionService
) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }


   val websockets = ReconnectingKtorWebsocket(
      discoveryClient,
      ServicesConfig.NEBULA_SERVER_NAME,
      1500.milliseconds,
      "/stream/stacks"
   )

   init {
      CoroutineScope(Dispatchers.IO).launch {
         websockets
            .connect()
            .collect { websocketSession ->
               logger.info { "Nebula client connected" }
               Flux.just(schemaWatcher.currentState).concatWith(schemaWatcher.stacksUpdated.map { it.currentState })
                  .subscribe { event ->
                     logger.info { "Sending updated stack to Nebula" }
                     val updatePayloadRequest =
                        UpdateStackRSocketRequest(stacks = event.map { (_, versionedSource) ->
                           NebulaSubmissionService.stackNameForSource(versionedSource) to versionedSource.content
                        }.toMap())
                     runBlocking {
                        try {
                           websocketSession.send(Frame.Text(objectMapper.writeValueAsString(updatePayloadRequest)))
                        } catch (e: Exception) {
                           logger.warn { "Failed to send message to Nebula websocket - ${e.message}" }
                        }
                     }
                  }
               websocketSession.incoming.consumeAsFlow()
                  .collect { frame ->
                     require(frame is Frame.Text)
                     val eventJson = frame.readText()
                     logger.info { "UpdateEvent : $eventJson" }
                     handleEvent(eventJson)
                  }
            }
      }

   }

   private fun handleEvent(eventJson: String) {
      try {
         logger.info { "Received updated Nebula state: $eventJson" }
         val event = objectMapper.readValue<StackStateEvent>(eventJson)
         val envVariables = envVariableSource.updateEnvVariables(event.stackState)
         val stateUpdatedEvent = NebulaStateUpdatedEvent(
            stacks = event.stackState,
            environmentVariables = envVariables,
            hasPendingUpdates = true
         )
         nebulaSubmissionService.emitStateUpdate(stateUpdatedEvent)
      } catch (e: Exception) {
         logger.error(e) { "Failed to submit update of Nebula environment variables" }
      }


   }
}

