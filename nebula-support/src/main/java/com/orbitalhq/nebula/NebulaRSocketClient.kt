package com.orbitalhq.nebula

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.orbitalhq.http.ServicesConfig
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.ktor.client.rSocket
import io.rsocket.kotlin.payload.buildPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import mu.KotlinLogging
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.stereotype.Component
import kotlin.time.Duration.Companion.milliseconds

@Component
class NebulaRSocketClient(
   private val schemaWatcher: NebulaSchemaWatcher,
   private val discoveryClient: DiscoveryClient,
   private val envVariableSource: NebulaEnvVariableSource,
   private val objectMapper: ObjectMapper
) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   val rsocketClientFlow = ReconnectingKtorRSocket(
      discoveryClient,
      ServicesConfig.NEBULA_SERVER_NAME,
      1500.milliseconds,
      "/events"
   )

   init {
      GlobalScope.launch(Dispatchers.IO) {
         rsocketClientFlow.connect()
            .collect { rsocket ->
               logger.info { "Nebula client connected" }
               val currentState = schemaWatcher.currentState
               logger.info { "Sending nebula state snapshot on new connection" }
               val request =
                  UpdateStackRSocketRequest(stacks = currentState.mapValues { (stackName, versionedSource) ->
                     versionedSource.content
                  })

               val initialPayload = buildPayload {
                  objectMapper.writeValueAsString(request) }
               val payloadFlow = schemaWatcher.stacksUpdated.asFlow()
                  .map { event ->
                     val updatePayloadRequest =
                        UpdateStackRSocketRequest(stacks = event.currentState.mapValues { (_, versionedSource) -> versionedSource.content })
                     buildPayload { objectMapper.writeValueAsString(updatePayloadRequest) }
                  }
               rsocket.requestChannel(initialPayload, payloadFlow)
                  .collect { updateEvent ->
                     logger.info { "UpdateEvent : $updateEvent" }
                  }
            }
      }

   }
}
