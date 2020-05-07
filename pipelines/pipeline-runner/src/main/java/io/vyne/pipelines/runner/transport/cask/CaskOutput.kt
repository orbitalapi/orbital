package io.vyne.pipelines.runner.transport.cask

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.discovery.EurekaClient
import io.vyne.VersionedTypeReference
import io.vyne.models.TypedInstance
import io.vyne.pipelines.PipelineDirection
import io.vyne.pipelines.PipelineLogger
import io.vyne.pipelines.PipelineOutputTransport
import io.vyne.pipelines.PipelineTransportSpec
import io.vyne.pipelines.runner.jobs.RunnerJobController
import io.vyne.pipelines.runner.transport.PipelineOutputTransportBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.EmitterProcessor
import java.net.URI


@Component
open class CaskOutputBuilder(val objectMapper: ObjectMapper, val client: EurekaClient) : PipelineOutputTransportBuilder<CaskTransportOutputSpec> {
   override fun canBuild(spec: PipelineTransportSpec) = spec.type == CaskTransport.TYPE && spec.direction == PipelineDirection.OUTPUT

   @Value("\${service.cask.name}")
   private lateinit var caskServiceName: String

   override fun build(spec: CaskTransportOutputSpec): PipelineOutputTransport {
      val caskServer = client.getNextServerFromEureka(caskServiceName, false)

      var endpoint = with(caskServer) { "ws://$hostName:$port/cask/${spec.targetType.typeName.name}" }
      endpoint = "ws://echo.websocket.org"
      return CaskOutput(spec, objectMapper, endpoint)
   }
}

class CaskOutput(spec: CaskTransportOutputSpec, private val objectMapper: ObjectMapper, val endpoint: String) : PipelineOutputTransport {
   override val type: VersionedTypeReference = spec.targetType

   private val client = ReactorNettyWebSocketClient()
   private val output: EmitterProcessor<String> = EmitterProcessor.create<String>()

   init {
      val sessionMono = client.execute(URI(endpoint)) { session ->
         session.receive()
            .map { obj: WebSocketMessage -> obj.payloadAsText }
            .subscribeWith(output)
            .doOnNext { println("Echo from websocket: $it") }
            .doAfterTerminate { println("Websocket terminated") }
            .doOnError { "Websocket Error: $it" }
            .doOnComplete { "Websocket completed" }
            .then()
      }

      // FIXME
      output.doOnSubscribe { s -> sessionMono.subscribe()}.subscribe();
      output.subscribe()
   }

   override fun write(typedInstance: TypedInstance, logger: PipelineLogger) {

      val json = objectMapper.writeValueAsString(typedInstance.toRawObject())
      logger.debug { "Generated json: $json" }
      logger.info { "Sending instance ${typedInstance.type.fullyQualifiedName} to Cask" }
      output.onNext(json);

   }




}
