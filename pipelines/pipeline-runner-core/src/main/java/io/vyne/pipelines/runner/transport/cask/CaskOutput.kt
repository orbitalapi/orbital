package io.vyne.pipelines.runner.transport.cask

import io.vyne.VersionedTypeReference
import io.vyne.pipelines.EmitterPipelineTransportHealthMonitor
import io.vyne.pipelines.MessageContentProvider
import io.vyne.pipelines.PipelineDirection
import io.vyne.pipelines.PipelineLogger
import io.vyne.pipelines.PipelineOutputTransport
import io.vyne.pipelines.PipelineTransportHealthMonitor
import io.vyne.pipelines.PipelineTransportHealthMonitor.PipelineTransportStatus.DOWN
import io.vyne.pipelines.PipelineTransportHealthMonitor.PipelineTransportStatus.TERMINATED
import io.vyne.pipelines.PipelineTransportHealthMonitor.PipelineTransportStatus.UP
import io.vyne.pipelines.PipelineTransportSpec
import io.vyne.pipelines.runner.netty.BackportReactorNettyWebsocketClient
import io.vyne.pipelines.runner.transport.PipelineOutputTransportBuilder
import io.vyne.pipelines.runner.transport.PipelineTransportFactory
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.utils.log
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.client.WebSocketClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI
import java.net.URLEncoder
import java.time.Duration.ofMillis
import java.util.*
import java.util.concurrent.Executors


@Component
class CaskOutputBuilder(
   val client: DiscoveryClient,
   @Value("\${vyne.caskService.name}") var caskServiceName: String,
   val healthMonitor: PipelineTransportHealthMonitor = EmitterPipelineTransportHealthMonitor(),
   private val wsClient: WebSocketClient = BackportReactorNettyWebsocketClient(),
   private val pollIntervalMillis: Long = 3000
) : PipelineOutputTransportBuilder<CaskTransportOutputSpec> {

   override fun canBuild(spec: PipelineTransportSpec) = spec.type == CaskTransport.TYPE && spec.direction == PipelineDirection.OUTPUT

   override fun build(spec: CaskTransportOutputSpec, logger: PipelineLogger, transportFactory: PipelineTransportFactory): PipelineOutputTransport {
      return CaskOutput(spec, logger, client, caskServiceName, healthMonitor, wsClient, pollIntervalMillis)
   }

}

class CaskOutput(
   val spec: CaskTransportOutputSpec,
   val logger: PipelineLogger,
   private val discoveryClient: DiscoveryClient,
   private val caskServiceName: String,
   override val healthMonitor: PipelineTransportHealthMonitor = EmitterPipelineTransportHealthMonitor(),
   private val wsClient: WebSocketClient = BackportReactorNettyWebsocketClient(),
   private val pollIntervalMillis: Long = 3000
) : PipelineOutputTransport {

   override val description: String = spec.description

   override fun type(schema: Schema): Type {
      return schema.type(spec.targetType)
   }

   private val CASK_CONTENT_TYPE_PARAMETER = "content-type"

   val messageHandler = CaskOutputMessageProvider(Executors.newSingleThreadExecutor())
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
            "ws://$host:$port/cask/${contentType}/${spec.targetType.typeName.fullyQualifiedName}${params}"

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
         val parameterName = it.key.removePrefix("${contentType}.")
         if (parameterName == "nullValue") {
            it.value.split(",").map { nullValue -> parameterName to nullValue }
         } else {
            listOf(parameterName to it.value)
         }
      }
      .sortedBy { it.first }
      .joinToString(separator = "&", prefix = "?") { e -> e.first + "=" + URLEncoder.encode(e.second, "UTF-8") }

   private fun handleWebsocketTermination(throwable: Throwable?) {
      messageHandler.write(PoisonPill())
      log().info("Websocket terminated: ${throwable?.message ?: "Unknown reason"}")
      healthMonitor.reportStatus(DOWN)
      tryToRestart()
   }

   /**
    * Initiate a websocket connection to the Cask server
    */
   private fun connectTo(endpoint: String) {


      // Connect to the websocket
      wsClient.execute(URI(endpoint),
         CaskWebsocketHandler(logger, healthMonitor, spec.targetType, messageHandler) { handleWebsocketTermination(it) })
         .doOnError {
            log().error("Could not connect to CASK. Handshake error.", it)
            healthMonitor.reportStatus(DOWN) // Handshake error = terminated (down for now as terminated is not handled)
            tryToRestart()
         }.subscribe()
   }

   private fun tryToRestart() {
      log().info("Initiating (re)connection to $caskServiceName service")
      findCaskEndpoint()
         .doOnError { healthMonitor.reportStatus(TERMINATED) }
         .doOnSuccess { connectTo(it) }.subscribe()
   }


   override fun write(message: MessageContentProvider, logger: PipelineLogger, schema: Schema) {
      logger.info { "Enqueuing message to send to Cask" }
      messageHandler.write(message)
   }
}

class CaskWebsocketHandler(
   val logger: PipelineLogger,
   val healthMonitor: PipelineTransportHealthMonitor,
   val versionedTypeReference: VersionedTypeReference,
   val caskOutputMessageProvider: CaskOutputMessageProvider,
   val onTermination: (throwable: Throwable?) -> Unit
) : WebSocketHandler {
   override fun handle(session: WebSocketSession): Mono<Void> {
      // At this point, this handshake is established!
      // ENHANCE: There might be a better place to hook on for this status
      healthMonitor.reportStatus(UP)

      val messageFlux = Flux.create(caskOutputMessageProvider).share()
      // Configure the session: inbounds and outbounds messages
      return session.send(messageFlux.map { messageContentProvider ->
         session.binaryMessage { factory ->
            val dataBuffer = factory.allocateBuffer()

            messageContentProvider.writeToStream(logger, dataBuffer.asOutputStream())
            dataBuffer
         }
      }).doOnSubscribe { logger.info { "subscribed on session ${session.id} for $versionedTypeReference" } }
         .and(
            session.receive().map { it.payloadAsText }
               .doOnNext {
                  logger.error { "Received response from websocket: $it" }
               }.then()
         )
         .doOnError { onTermination(it) }
         .doOnSuccess { onTermination(null) } // Is this ever called ?
         .then()
   }

}




