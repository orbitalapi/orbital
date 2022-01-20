package io.vyne.httpSchemaPublisher

import io.vyne.schemaPublisherApi.HttpPollKeepAlive
import io.vyne.schemaPublisherApi.KeepAliveStrategy
import io.vyne.schemaPublisherApi.KeepAliveStrategyMonitor
import io.vyne.schemaPublisherApi.PublisherConfiguration
import mu.KotlinLogging
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.SignalType
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.time.Instant
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

private val logger = KotlinLogging.logger { }

/**
 * Handles keep alive logic for schema publishers with @see HttpPollKeepAliveStrategyMonitor strategy.
 */
class HttpPollKeepAliveStrategyMonitor(ttlCheckPeriod: Duration = Duration.ofSeconds(1L),
                                       private val httpRequestTimeoutInSeconds: Long = 30L,
                                       pollUrlResolver: HttpPollKeepAliveStrategyPollUrlResolver = HttpPollKeepAliveStrategyPollUrlResolver(Optional.empty()),
                                       scheduler: Scheduler = Schedulers.single(),
                                       internal val lastPingTimes: ConcurrentMap<PublisherConfiguration, Instant> = ConcurrentHashMap(),
                                       private val webClientBuilder: WebClient.Builder) : KeepAliveStrategyMonitor {
   private val sink = Sinks.many().multicast()
      .onBackpressureBuffer<PublisherConfiguration>()

   override val terminatedInstances: Flux<PublisherConfiguration> = sink.asFlux()

   init {
      if (ttlCheckPeriod.isZero) {
         logger.warn { "Polling for expired sources has been disabled!" }
      } else {
         Flux.interval(ttlCheckPeriod, scheduler)
            .subscribe {
               lastPingTimes.forEach { (publisherConfig, lastTimeStamp) ->
                  val now = Instant.now()
                  (publisherConfig.keepAlive as HttpPollKeepAlive).run {
                     if (lastTimeStamp <= now.minusSeconds(pollFrequency.toSeconds())) {
                        // set the lastUpdated time for the entry
                        lastPingTimes[publisherConfig] = Instant.MAX
                        logger.info { "Performing keep alive check for $publisherConfig" }
                        webClientBuilder
                           .build()
                           .get()
                           .uri(pollUrlResolver.absoluteUrl(pollUrl))
                           .retrieve()
                           .toBodilessEntity()
                           .timeout(Duration.ofSeconds(httpRequestTimeoutInSeconds))
                           .map { entity ->
                              if (!entity.statusCode.is2xxSuccessful) {
                                 logger.warn { "Keep alive call for $publisherConfig returned ${entity.statusCode} so marking the publisher as zombie" }
                                 lastPingTimes.remove(publisherConfig)?.let { _ ->
                                    sink.emitNext(publisherConfig, RetryFailOnSerializeEmitHandler)
                                 }
                              } else {
                                 lastPingTimes[publisherConfig] = Instant.now()
                              }
                           }
                           .doOnError { error ->
                              logger.error(error) {
                                 "Keep alive call for $publisherConfig returned Error so marking the publisher as zombie"
                              }
                              lastPingTimes.remove(publisherConfig)?.let { _ ->
                                 sink.emitNext(publisherConfig, RetryFailOnSerializeEmitHandler)
                              }
                           }.subscribe()
                     }
                  }
               }
            }
      }
   }

   override fun appliesTo(keepAlive: KeepAliveStrategy) = keepAlive is HttpPollKeepAlive

   override fun monitor(publisherConfiguration: PublisherConfiguration) {
      logger.debug { "Monitoring keep alive for $publisherConfiguration" }
      lastPingTimes[publisherConfiguration] = Instant.now()
   }
}

object RetryFailOnSerializeEmitHandler: Sinks.EmitFailureHandler {
   override fun onEmitFailure(signalType: SignalType, emitResult: Sinks.EmitResult) = emitResult == Sinks.EmitResult.FAIL_NON_SERIALIZED
}

class HttpPollKeepAliveStrategyPollUrlResolver(private val discoveryClient: Optional<DiscoveryClient>) {
   fun absoluteUrl(pollUrl: String): String {
      val uriComponents = UriComponentsBuilder.fromUriString(pollUrl).build()
      val scheme = uriComponents.scheme
      return when {
         scheme != null && scheme.equals("http", true) || scheme.equals("https:", true) -> uriComponents.toUriString()
         discoveryClient.isPresent -> {
            val uriComponents = UriComponentsBuilder.fromUriString(pollUrl).build()
            val instanceId = uriComponents.pathSegments.first()
            val uriParts = uriComponents.pathSegments.drop(1)
            discoveryClient
               .get()
               .getInstances(instanceId)
               .firstOrNull()
               ?.uri
               ?.toASCIIString()
               ?.let { baseUrl ->
                  UriComponentsBuilder.fromHttpUrl(baseUrl).pathSegment(*uriParts.toTypedArray()).toUriString()
               } ?: pollUrl
         }
         else -> pollUrl
      }
   }
}
