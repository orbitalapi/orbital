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

class CaskOutput(spec: CaskTransportOutputSpec, private val objectMapper: ObjectMapper, private val eurekaClient: EurekaClient, private val caskServiceName: String) : PipelineOutputTransport, AbstractPipelineTransportHealthMonitor() {
   override val type: VersionedTypeReference = spec.targetType

   private val client = ReactorNettyWebSocketClient()
   private val output = EmitterProcessor.create<String>()

   init {
      findCaskEndpoint().subscribe { connectTo(it) }
   }

   private fun findCaskEndpoint(): Mono<String> {
      // ENHANCE: we might not want to poll indefinitelyhere
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
      try {
         val caskServer = eurekaClient.getNextServerFromEureka(caskServiceName, false)
         var endpoint = with(caskServer) { "ws://$hostName:$port/cask/${type.typeName.fullyQualifiedName}" }
         log().info("Found for $caskServiceName service server in Eureka [endpoint=$endpoint]")
         return Mono.just(endpoint)
      } catch (e: RuntimeException) {
         // FIXME check if more fine grained exception is possible
         log().info("Could not find $caskServiceName server. Reason: ${e.message}")
         return Mono.empty()
      }
   }

   /**
    * Initiate a websocket connection to the Cask server
    */
   private fun connectTo(endpoint: String){
      var handshakeMono = client.execute(URI(endpoint)) { session ->
         // At this point, this handshake is established!
         // ENHANCE: There might be a better place to hook on for this status
         reportStatus(UP)

         // Configure the session: inbounds and outbounds messages
         session.send(
            output.map { session.textMessage(it) }
         )
         .and(
            session.receive().handleCaskResponse().then()
         )
         .doOnError { handleWebsocketTermination(it) }
         .doOnSuccess { handleWebsocketTermination(null) }
         //.log("CASK")
         .then()
      }
      .log("HANDSHAKE")
         .doOnSuccess { println("SUCCESS")}
         .doOnError { handleWebsocketHandshakeError(it) }
      output.doOnSubscribe { handshakeMono.subscribe() }
         //.log("OUTPUT")
         .subscribe()
   }

   private fun handleWebsocketHandshakeError(throwable: Throwable) {
      log().info("Websocket connection error: ${throwable?.message}")
      tryToRestart()
   }

   private fun handleWebsocketTermination(throwable: Throwable?) {
      log().info("Websocket terminated: ${throwable?.message}")
      tryToRestart()
   }

   private fun tryToRestart() {
      log().info("Initiating reconnection to $caskServiceName service")
      reportStatus(DOWN)
      findCaskEndpoint().subscribe { connectTo(it) }
   }


   private fun Flux<WebSocketMessage>.handleCaskResponse(): Flux<CaskIngestionResponse> {

      // For now just log
      return map { it.payloadAsText }
         .map { objectMapper.readValue(it, CaskIngestionResponse::class.java) }
         .doOnNext {
            it.log().info("Received response from websocket: ${it.result}")
         }
   }

   override fun write(typedInstance: TypedInstance, logger: PipelineLogger) {
      val json = objectMapper.writeValueAsString(typedInstance.toRawObject())
      logger.info { "Sending instance ${typedInstance.type.fullyQualifiedName} to Cask" }
      output.onNext(json);
   }
}




