package io.vyne.pipelines.runner.transport.cask

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.discovery.EurekaClient
import io.vyne.VersionedTypeReference
import io.vyne.models.TypedInstance
import io.vyne.pipelines.PipelineDirection
import io.vyne.pipelines.PipelineLogger
import io.vyne.pipelines.PipelineOutputTransport
import io.vyne.pipelines.PipelineTransportSpec
import io.vyne.pipelines.runner.transport.PipelineOutputTransportBuilder
import io.vyne.utils.log
import org.reactivestreams.Subscription
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.EmitterProcessor
import java.net.URI


@Component
class CaskOutputBuilder(val objectMapper: ObjectMapper, val client: EurekaClient, @Value("\${service.cask.name}") var caskServiceName: String) : PipelineOutputTransportBuilder<CaskTransportOutputSpec> {
   override fun canBuild(spec: PipelineTransportSpec) = spec.type == CaskTransport.TYPE && spec.direction == PipelineDirection.OUTPUT


   override fun build(spec: CaskTransportOutputSpec): PipelineOutputTransport {
      val caskServer = client.getNextServerFromEureka(caskServiceName, false)

      var endpoint = with(caskServer) { "ws://$hostName:$port/cask/${spec.targetType.typeName.fullyQualifiedName}" }
      //endpoint = "ws://echo.websocket.org" // FOR TESTS
      return CaskOutput(spec, objectMapper, endpoint)
   }
}

class CaskOutput(spec: CaskTransportOutputSpec, private val objectMapper: ObjectMapper, val endpoint: String) : PipelineOutputTransport {
   override val type: VersionedTypeReference = spec.targetType

   private val client = ReactorNettyWebSocketClient()
   private val output: EmitterProcessor<String> = EmitterProcessor.create<String>()

   init {
      val sessionMono = client.execute(URI(endpoint)
      ) { session: WebSocketSession ->
         session.send(output.map(session::textMessage))
            .doOnNext { log().info("Next") }
            .doAfterTerminate { log().info("Websocket terminated") }
            .doOnError { log().info("Websocket Error: $it") }
            .then()
         // FIXME add receive
      }

      output.doOnSubscribe { s: Subscription? -> sessionMono.subscribe() }.subscribe()
   }

   override fun write(typedInstance: TypedInstance, logger: PipelineLogger) {

      val json = objectMapper.writeValueAsString(typedInstance.toRawObject())
      logger.debug { "Generated json: $json" }
      logger.info { "Sending instance ${typedInstance.type.fullyQualifiedName} to Cask" }
      output.onNext(json);

   }

}
