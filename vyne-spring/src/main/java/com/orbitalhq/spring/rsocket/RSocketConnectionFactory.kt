package com.orbitalhq.spring.rsocket

import com.orbitalhq.connections.ConnectionStatus
import com.orbitalhq.connections.ConnectionStatus.*
import com.orbitalhq.http.ServicesConfig
import mu.KotlinLogging
import org.reactivestreams.Publisher
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.http.MediaType
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.util.retry.Retry
import java.net.URI
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass

/**
 * An RSocket factory which creates a reconnecting client using a discovery client.
 *
 */
@Component
class RSocketConnectionFactory(private val rsocketStrategies: RSocketStrategies) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   private val instanceIndexMap = mutableMapOf<String, AtomicInteger>()

   fun <T : Any> reconnectingRSocket(
      applicationName: String,
      discoveryClient: DiscoveryClient,
      route: String,
      dataPublisher: Publisher<Any>,
      payloadClass: KClass<T>,
      mimeType: MediaType = MediaType.APPLICATION_JSON,
   ): Pair<Flux<T>, Flux<ConnectionStatus>> {
      val statusMessageSink = Sinks.many().replay().latest<ConnectionStatus>()

      val messages = Flux.defer {
         getRSocketAddress(applicationName, discoveryClient)
            .flatMapMany { address ->
               connectAndRetrieve(
                  applicationName,
                  address,
                  route,
                  dataPublisher,
                  payloadClass,
                  mimeType,
                  statusMessageSink
               )
            }
      }
         .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
            .maxBackoff(Duration.ofSeconds(10))
            .doBeforeRetry { retrySignal ->
               logger.info { "Attempting to reconnect, retry #${retrySignal.totalRetries()}" }
            })

      return messages to statusMessageSink.asFlux()
   }


   private fun <T : Any> connectAndRetrieve(
      applicationName: String,
      address: Pair<String, Int>,
      route: String,
      dataPublisher: Publisher<Any>,
      payloadClass: KClass<T>,
      mimeType: MediaType,
      statusMessageSink: Sinks.Many<ConnectionStatus>
   ): Flux<T> {
      val (host, port) = address
      logger.info { "Attempting to build an RSocket to $applicationName at $host:$port" }
      val lastConnectionState = AtomicReference<Status>(Status.ERROR)

      return RSocketRequester.builder()
         .dataMimeType(mimeType)
         .rsocketStrategies(rsocketStrategies)
         .tcp(host, port)
         .route(route)
         .data(dataPublisher)
         .retrieveFlux(payloadClass.java)
         .doOnSubscribe {
            lastConnectionState.set(Status.CONNECTING)
            statusMessageSink.tryEmitNext(
               ConnectionStatus(
                  status = Status.CONNECTING,
                  message = "Attempting to establish connection to $applicationName at $host:$port"
               )
            )
         }
         .doOnNext {
            if (lastConnectionState.getAndUpdate { Status.OK } != Status.OK) {
               statusMessageSink.tryEmitNext(
                  ConnectionStatus(
                     status = Status.OK,
                     message = "Connected"
                  )
               )
            }
         }
         .doOnError { error ->
            logger.info { "Error on rSocket connection to $applicationName: ${error.message}" }
            lastConnectionState.set(Status.ERROR)
            statusMessageSink.tryEmitNext(
               ConnectionStatus(
                  status = Status.ERROR,
                  message = error.message ?: "An unknown error occurred"
               )
            )
         }
   }

   private fun getRSocketAddress(applicationName: String, discoveryClient: DiscoveryClient): Mono<Pair<String, Int>> {
      return Mono.defer {
         val instances = discoveryClient.getInstances(applicationName)
         if (instances.isEmpty()) {
            return@defer Mono.error(IllegalStateException("Can't find any $applicationName instance registered in DiscoveryClient"))
         }

         val index = instanceIndexMap.computeIfAbsent(applicationName) { AtomicInteger(0) }
         val instance = instances[index.getAndIncrement() % instances.size]

         val rsocketConnection = instance.metadata[ServicesConfig.RSOCKET]
            ?: return@defer Mono.error(IllegalStateException("The $applicationName instance does not expose an rsocket address - check your services.conf file"))

         val rsocketUri = URI.create(rsocketConnection)
         Mono.just(Pair(rsocketUri.host, rsocketUri.port))
      }

   }
}
