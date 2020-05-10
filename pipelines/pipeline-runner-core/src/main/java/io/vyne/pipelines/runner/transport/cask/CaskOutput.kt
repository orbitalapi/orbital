package io.vyne.pipelines.runner.transport.cask

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.discovery.EurekaClient
import io.vyne.VersionedTypeReference
import io.vyne.cask.api.CaskIngestionResponse
import io.vyne.models.TypedInstance
import io.vyne.pipelines.PipelineDirection
import io.vyne.pipelines.PipelineLogger
import io.vyne.pipelines.PipelineOutputTransport
import io.vyne.pipelines.PipelineTransportSpec
import io.vyne.pipelines.runner.transport.PipelineOutputTransportBuilder
import io.vyne.utils.log
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI


@Component
class CaskOutputBuilder(val objectMapper: ObjectMapper, val client: EurekaClient, @Value("\${service.cask.name}") var caskServiceName: String) : PipelineOutputTransportBuilder<CaskTransportOutputSpec> {
   override fun canBuild(spec: PipelineTransportSpec) = spec.type == CaskTransport.TYPE && spec.direction == PipelineDirection.OUTPUT

   override fun build(spec: CaskTransportOutputSpec): PipelineOutputTransport {
      val caskServer = client.getNextServerFromEureka(caskServiceName, false)

      var endpoint = with(caskServer) { "ws://$hostName:$port/cask/${spec.targetType.typeName.fullyQualifiedName}" }
      return CaskOutput(spec, objectMapper, endpoint)
   }
}

class CaskOutput(spec: CaskTransportOutputSpec, private val objectMapper: ObjectMapper, val endpoint: String) : PipelineOutputTransport {
   override val type: VersionedTypeReference = spec.targetType

   private val client = ReactorNettyWebSocketClient()
   private val output = EmitterProcessor.create<String>()

   init {
      var sessionMono = connect()
      output.doOnSubscribe { sessionMono.subscribe() }.subscribe()
   }

   override fun write(typedInstance: TypedInstance, logger: PipelineLogger) {
      val json = objectMapper.writeValueAsString(typedInstance.toRawObject())
      logger.info { "Sending instance ${typedInstance.type.fullyQualifiedName} to Cask" }
      output.onNext(json);
   }

   private fun connect(): Mono<Void> {
      return client.execute(URI(endpoint)) { session ->
         session.send(
            output.map { session.textMessage(it) }
         )
            .and(
               session.receive().handleCaskResponse().then()
            )
            .doOnError {
               // ENHANCE read error and recover if possible
               log().error("Websocket error", it)
            }
            .doAfterTerminate {
               // ENHANCE check reason (any code somewhere ?) and try to reconnect
               log().info("Websocket terminated ")
            }
            .then()
      }
   }


   private fun Flux<WebSocketMessage>.handleCaskResponse(): Flux<CaskIngestionResponse> {

      return map { it.payloadAsText }
         .map { objectMapper.readValue(it, CaskIngestionResponse::class.java) }
         .doOnNext {
            it.log().info("Received response from websocket: ${it.result}")
         }
   }
}



