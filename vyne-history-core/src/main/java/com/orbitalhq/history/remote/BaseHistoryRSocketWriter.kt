package com.orbitalhq.history.remote

import com.orbitalhq.history.QueryAnalyticsConfig
import mu.KotlinLogging
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.http.MediaType
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.net.URI
import java.time.Duration

/**
 * Base class for creating a writer that emits query history events
 * over RSocket.
 *
 * Different implementations for running within the QueryServer,
 * and running in a standalone query node
 */
abstract class BaseHistoryRSocketWriter(
   private val config: QueryAnalyticsConfig,
) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   fun buildRSocketPublisher(
      rsocketStrategies: RSocketStrategies,
      discoveryClient: DiscoveryClient,
      queryEventConsumer: RemoteQueryEventConsumerClient
   ): Flux<Void> {
      val responder =
         RSocketMessageHandler.responder(rsocketStrategies, queryEventConsumer)

      return resolveHistoryServerHostPort(discoveryClient)
         .flatMap { (host, port) ->
            RSocketRequester
               .builder()
               .dataMimeType(MediaType.APPLICATION_CBOR)
               .rsocketStrategies(rsocketStrategies)
               .rsocketConnector { connector ->
                  connector
                     .acceptor(responder)
               }
               .connectTcp(host, port)
               .flatMap { r ->
                  r.rsocket()
                     .onClose()
                     .doOnSubscribe {
                        logger.info { "Subscribed to OnClose" }
                     }
               }
         }
         .repeat()
         .retryWhen(
            Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(10))
               .doBeforeRetry { retrySignal: Retry.RetrySignal? ->
                  logger.warn { "Connection Closed re-trying {$retrySignal}" }
               })
         .doOnError {
            logger.warn {
               "Vyne Query History Service   connection is closed!"
            }
         }
         .doFinally {
            logger.warn {
               "Vyne Query History Service   connection is disconnected!"
            }
         }
   }

   private fun resolveHistoryServerHostPort(discoveryClient: DiscoveryClient): Mono<Pair<String, Int>> {
      return Mono.defer {
         val instances = discoveryClient.getInstances(config.analyticsServerApplicationName)
         if (instances.isEmpty()) {
            return@defer Mono.error(IllegalStateException("Can't find any ${config.analyticsServerApplicationName} instance registered in DiscoveryClient"))
         }
         // TODO : Consider round-robin with Ribbon, or to randomize
         val firstHistoryServiceInstance = instances.first()
         val rsocketConnection = firstHistoryServiceInstance.metadata["rsocket"]
            ?: return@defer Mono.error(IllegalStateException("The ${config.analyticsServerApplicationName} instance configured in DiscoveryClient does not expose an rsocket address"))
         val rsocketUri = URI.create(rsocketConnection)
         return@defer Mono.just(Pair(rsocketUri.host, rsocketUri.port))
      }


   }
}
