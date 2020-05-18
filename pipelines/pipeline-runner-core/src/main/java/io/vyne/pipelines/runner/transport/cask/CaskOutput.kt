package io.vyne.pipelines.runner.transport.cask

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.discovery.EurekaClient
import io.vyne.VersionedTypeReference
import io.vyne.cask.api.CaskIngestionResponse
import io.vyne.models.TypedInstance
import io.vyne.pipelines.*
import io.vyne.pipelines.PipelineTransportHealthMonitor.PipelineTransportStatus.DOWN
import io.vyne.pipelines.PipelineTransportHealthMonitor.PipelineTransportStatus.UP
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
import java.time.Duration.ofMillis


@Component
class CaskOutputBuilder(val objectMapper: ObjectMapper, val client: EurekaClient, @Value("\${service.cask.name}") var caskServiceName: String) : PipelineOutputTransportBuilder<CaskTransportOutputSpec> {

   override fun canBuild(spec: PipelineTransportSpec) = spec.type == CaskTransport.TYPE && spec.direction == PipelineDirection.OUTPUT

   override fun build(spec: CaskTransportOutputSpec): PipelineOutputTransport = CaskOutput(spec, objectMapper, client, caskServiceName)

}

class CaskOutput(spec: CaskTransportOutputSpec, private val objectMapper: ObjectMapper, private val eurekaClient: EurekaClient, private val caskServiceName: String) : PipelineOutputTransport, DefaultPipelineTransportHealthMonitor() {
   override val type: VersionedTypeReference = spec.targetType

   private val wsClient = ReactorNettyWebSocketClient()
   private val wsOutput = EmitterProcessor.create<String>()

   init {
      tryToRestart()
   }

   private fun findCaskEndpoint(): Mono<String> {
      // ENHANCE: we might not want to poll indefinitely here
      // add .take(X) to limit that, and create new status TERMINATED for this transport ?
      return Flux.interval(ofMillis(3000))
         .onBackpressureDrop()
         .flatMap { pollCaskServiceServer() }
         .next()
   }

   /**
    * Poll Eureka for the next Cask server available
    */
   private fun pollCaskServiceServer(): Mono<String> {
      return try {
         val caskServer = eurekaClient.getNextServerFromEureka(caskServiceName, false)
         val endpoint = with(caskServer) { "ws://$hostName:$port/cask/${type.typeName.fullyQualifiedName}" }
         log().info("Found for $caskServiceName service server in Eureka [endpoint=$endpoint]")
         Mono.just(endpoint)
      } catch (e: RuntimeException) {
         // ENHANCE check if more fine grained exception is possible ?
         log().info("Could not find $caskServiceName server. Reason: ${e.message}")
         Mono.empty()
      }
   }

   /**
    * Initiate a websocket connection to the Cask server
    */
   private fun connectTo(endpoint: String) {
      val handshakeMono = wsClient.execute(URI(endpoint)) { session ->
         // At this point, this handshake is established!
         // ENHANCE: There might be a better place to hook on for this status
         reportStatus(UP)

         // Configure the session: inbounds and outbounds messages
         session.send(wsOutput.map { session.textMessage(it) })
            .and(
               session.receive().handleCaskResponse().then()
            )
            .doOnError { handleWebsocketTermination(it) }
            .doOnSuccess { handleWebsocketTermination(null) } // Is this ever called ?
            .then()
      }
         .doOnError { handleWebsocketTermination(it) }

      // Subscribe to all
      wsOutput.doOnSubscribe { handshakeMono.subscribe() }.subscribe()
   }

   private fun handleWebsocketTermination(throwable: Throwable?) {
      log().info("Websocket terminated: ${throwable?.message ?: "Unknown reason"}")
      reportStatus(DOWN)
      tryToRestart()
   }

   private fun tryToRestart() {
      log().info("Initiating (re)connection to $caskServiceName service")
      findCaskEndpoint().doOnSuccess { connectTo(it) }.subscribe()
   }

   private fun Flux<WebSocketMessage>.handleCaskResponse(): Flux<String> {

      // For now just log
      // LENS-50 - cask will return the message in case of error
      return map { it.payloadAsText }
         .doOnNext {
            it.log().info("Received response from websocket: $it")
         }
   }

   override fun write(typedInstance: TypedInstance, logger: PipelineLogger) {
      val json = objectMapper.writeValueAsString(typedInstance.toRawObject())
      logger.info { "Sending instance ${typedInstance.type.fullyQualifiedName} to Cask" }
      wsOutput.onNext(json);
   }

}




