package io.vyne.pipelines.runner.transport.cask

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.VersionedTypeReference
import io.vyne.pipelines.*
import io.vyne.pipelines.PipelineTransportHealthMonitor.PipelineTransportStatus.*
import io.vyne.pipelines.runner.transport.PipelineOutputTransportBuilder
import io.vyne.utils.log
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import org.springframework.web.reactive.socket.client.WebSocketClient
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI
import java.net.URLEncoder
import java.time.Duration.ofMillis
import java.util.*


@Component
class CaskOutputBuilder(val objectMapper: ObjectMapper, val client: DiscoveryClient, @Value("\${service.cask.name}") var caskServiceName: String) : PipelineOutputTransportBuilder<CaskTransportOutputSpec> {

   override fun canBuild(spec: PipelineTransportSpec) = spec.type == CaskTransport.TYPE && spec.direction == PipelineDirection.OUTPUT

   override fun build(spec: CaskTransportOutputSpec, logger: PipelineLogger): PipelineOutputTransport = CaskOutput(spec, logger, client, caskServiceName)

}

class CaskOutput(
   val spec: CaskTransportOutputSpec,
   logger: PipelineLogger,
   private val discoveryClient: DiscoveryClient,
   private val caskServiceName: String,
   override val healthMonitor: PipelineTransportHealthMonitor = EmitterPipelineTransportHealthMonitor(),
   private val wsClient: WebSocketClient = ReactorNettyWebSocketClient(),
   private val pollIntervalMillis: Long = 3000
) : PipelineOutputTransport {

   override val type: VersionedTypeReference = spec.targetType

   private val CASK_CONTENT_TYPE_PARAMETER = "content-type"

   val wsOutput: EmitterProcessor<String> = EmitterProcessor.create()
   val wsHandler = CaskWebsocketHandler(logger, healthMonitor, wsOutput) { handleWebsocketTermination(it) }

   init {
      tryToRestart()
   }

   private fun findCaskEndpoint(): Mono<String> {
      // ENHANCE: we might not want to poll indefinitely here
      // add .take(X) to limit that, and create new status TERMINATED for this transport ?
      return Flux.interval(ofMillis(pollIntervalMillis))
         .onBackpressureDrop()
         .map { getCaskServiceEndpoint() }
         .filter { it.isPresent }
         .map { it.get() }
         .next()
   }

   /**
    * Poll Eureka for the next Cask server available
    */
   private fun getCaskServiceEndpoint(): Optional<String> {
      return try {
         val caskServers = discoveryClient.getInstances(caskServiceName)

         if (caskServers.isEmpty()) {
            log().error("Could not find $caskServiceName server. Reason: No cask instances running.")
            return Optional.empty()
         }
         val caskServer = caskServers.random()  // ENHANCE client side load balancing ?

         // Build the WS connection parameters
         val contentType = spec.props[CASK_CONTENT_TYPE_PARAMETER] ?: "json"
         val params = buildWsParameters(spec, contentType)

         // Build th final endpoint
         val endpoint = with(caskServer) {
            "ws://$host:$port/cask/${contentType}/${type.typeName.fullyQualifiedName}${params}"

         }
         log().info("Found $caskServiceName service server in Service Discovery [endpoint=$endpoint]")
         Optional.of(endpoint)
      } catch (e: RuntimeException) {
         log().error("Could not find $caskServiceName server. Reason: ${e.message}")
         Optional.empty()
      }
   }

   private fun buildWsParameters(spec: CaskTransportOutputSpec, contentType: String) = spec.props.entries
      .filter { it.key.startsWith("${contentType}.") }
      .flatMap {
         var parameterName = it.key.removePrefix("${contentType}.")
         if (parameterName == "nullValue") {
            it.value.split(",").map { nullValue -> parameterName to nullValue }
         } else {
            listOf(parameterName to it.value)
         }
      }
      .sortedBy { it.first }
      .joinToString(separator = "&", prefix = "?") { e -> e.first + "=" + URLEncoder.encode(e.second, "UTF-8") }

   private fun handleWebsocketTermination(throwable: Throwable?) {
      log().info("Websocket terminated: ${throwable?.message ?: "Unknown reason"}")
      healthMonitor.reportStatus(DOWN)
      tryToRestart()
   }

   /**
    * Initiate a websocket connection to the Cask server
    */
   private fun connectTo(endpoint: String) {


      // Connect to the websocket
      val handshakeMono = wsClient.execute(URI(endpoint), wsHandler)
         .doOnError { healthMonitor.reportStatus(DOWN) } // Handshake error = terminated (down for now as terminated is not handled)

      // Subscribe to all
      wsOutput.doOnSubscribe { handshakeMono.subscribe() }.subscribe()
   }

   private fun tryToRestart() {
      log().info("Initiating (re)connection to $caskServiceName service")
      findCaskEndpoint()
         .doOnError { healthMonitor.reportStatus(TERMINATED) }
         .doOnSuccess { connectTo(it) }.subscribe()
   }


   override fun write(message: String, logger: PipelineLogger) {
      logger.info { "Sending message to Cask" }
      wsOutput.onNext(message)
   }

}

class CaskWebsocketHandler(
   val logger: PipelineLogger,
   val healthMonitor: PipelineTransportHealthMonitor,
   val wsOutput: EmitterProcessor<String>,
   val onTermination: (throwable: Throwable?) -> Unit
) : WebSocketHandler {
   override fun handle(session: WebSocketSession): Mono<Void> {
      // At this point, this handshake is established!
      // ENHANCE: There might be a better place to hook on for this status
      healthMonitor.reportStatus(UP)

      // Configure the session: inbounds and outbounds messages
      return session.send(wsOutput.map { session.textMessage(it) })
         .and(
            session.receive().map { it.payloadAsText }
               .doOnNext {
                  logger.error { "Received response from websocket: $it"}
                  it.log().error( "Received response from websocket: $it")
               }.then()
         )
         .doOnError { onTermination(it) }
         .doOnSuccess { onTermination(null) } // Is this ever called ?
         .then()
   }

}




