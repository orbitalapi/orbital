package io.vyne.schemeRSocketCommon

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.schemaApi.SchemaSet
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.http.MediaType
import org.springframework.http.codec.cbor.Jackson2CborDecoder
import org.springframework.http.codec.cbor.Jackson2CborEncoder
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.web.util.pattern.PathPatternRouteMatcher
import reactor.core.publisher.Mono
import reactor.core.publisher.SignalType
import reactor.core.publisher.Sinks
import reactor.util.retry.Retry
import java.time.Duration
import java.util.function.Consumer

private val logger = KotlinLogging.logger {  }


class RSocketSchemaServerProxy(
   @Value("\${vyne.schema-server.name:schema-server}") private val schemaServerApplicationName: String,
   private val discoveryClient: DiscoveryClient,
   private val objectMapper: ObjectMapper = jacksonObjectMapper()) {

   fun consumeSchemaSets(consumer: Consumer<SchemaSet>) {
      resolveSchemaServerEndPoint(discoveryClient, schemaServerApplicationName)
         .flatMap { (host, port) ->
            RSocketRequester
               .builder()
               .dataMimeType(MediaType.APPLICATION_CBOR)
               .rsocketStrategies(rsocketStrategies())
               .connectTcp(host, port)
               .flatMap { r ->
                  r.route("stream.vyneSchemaSets").retrieveFlux(SchemaSet::class.java).subscribe(consumer)
                  r.rsocket()
                     .onClose()
                     .doOnSubscribe {
                        logger.info { "Schema Server RSocket Connection closed Whilst consuming Schemas." }
                     }
               }
         }
         .repeat() // Handles the case where connection established but then server is closed, r.rsocket().Close() will trigger the repeat() for re-connection.
         .retryWhen(Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(10)) // If there is an error for 'connectTcp(..)', this will re-try.
            .doBeforeRetry { retrySignal: Retry.RetrySignal? ->
               logger.warn {"Schema Server Connection Closed, re-trying to connect {$retrySignal}"}
            })
         .doOnError { logger.warn {
            "Schema Server connection is closed!" } }
         .doFinally { logger.warn {
            "Schema Server  connection is disconnected!" } }
         .subscribe()
   }

   fun <T> schemaServerPublishSchemaConnection(setUpData: T, schemaSerDisconnectionSink: Sinks.Many<Unit>): Mono<RSocketRequester> {
      val setUpDataJsonStr = objectMapper.writeValueAsString(setUpData)
      return resolveSchemaServerEndPoint(discoveryClient, schemaServerApplicationName)
         .flatMap { (host, port) ->
            RSocketRequester
               .builder()
               .rsocketConnector { connector ->
                  connector.reconnect(Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(10))
                     .doBeforeRetry { retrySignal: Retry.RetrySignal? ->
                        logger.warn {"Schema Server Connection Closed, re-trying to connect {$retrySignal}"}
                     }
                  )
               }
               .dataMimeType(MediaType.APPLICATION_CBOR)
               .rsocketStrategies(rsocketStrategies())
               .setupData(setUpDataJsonStr)
               .connectTcp(host, port)
               .map { requester ->
                  requester.rsocket().onClose().doFirst {
                  }.doOnError { error ->
                     logger.warn { "Schema Server Connection is DROPPED! $error" }
                  }.doFinally { signalType ->
                     logger.warn { "Schema Server Connection is DROPPED! ${signalType.name}" }
                     if (signalType == SignalType.ON_COMPLETE) {
                        schemaSerDisconnectionSink.tryEmitNext(Unit)
                     }
                  }
                     .subscribe()
                  requester
               }
         }
   }

   private fun rsocketStrategies() = RSocketStrategies.builder()
      .encoders { it.add(Jackson2CborEncoder()) }
      .decoders { it.add(Jackson2CborDecoder()) }
      .routeMatcher(PathPatternRouteMatcher())
      .build()

   private fun resolveSchemaServerEndPoint(discoveryClient: DiscoveryClient, schemaServerApplicationName: String): Mono<Pair<String, Int>> {
      return Mono.defer {
         val instances = discoveryClient.getInstances(schemaServerApplicationName)
         if (instances.isEmpty()) {
            return@defer Mono.error(IllegalStateException("Can't find any $schemaServerApplicationName instance registered on Service Discovery Registry"))
         }
         // TODO : Consider round-robin with Ribbon, or to randomize
         val firstHistoryServiceInstance = instances.first()
         val schemaServiceHost = firstHistoryServiceInstance.host
         val schemaServicePort = firstHistoryServiceInstance.metadata["vyne-schema-server-port"] ?: "80"
         return@defer Mono.just(Pair(schemaServiceHost, schemaServicePort.toInt()))
      }
   }

}
