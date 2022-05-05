package io.vyne.schema.publisher.http

import io.vyne.schema.publisher.HttpPollKeepAlive
import io.vyne.schema.publisher.KeepAliveStrategy
import io.vyne.schema.publisher.KeepAliveStrategyMonitor
import io.vyne.schema.publisher.PublisherConfiguration
import mu.KotlinLogging
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.*
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

private val logger = KotlinLogging.logger { }

/**
 * Handles keep alive logic for schema publishers with @see HttpPollKeepAliveStrategyMonitor strategy.
 */
class HttpPollKeepAliveStrategyMonitor(
   // The frequency that we run checks looking for services to poll.
   // Individual services are polled based on their own poll frequency as defined in their
   // HttpKeepAlive config
   pollFrequency: Duration = Duration.ofSeconds(1L),
   private val httpRequestTimeout: Duration = Duration.ofSeconds(30),
   scheduler: Scheduler = Schedulers.single(),
   internal val lastPingTimes: ConcurrentMap<PublisherConfiguration, Instant> = ConcurrentHashMap(),
   private val webClientBuilder: WebClient.Builder
) : KeepAliveStrategyMonitor {
   private val sink = Sinks.many().multicast()
      .onBackpressureBuffer<PublisherConfiguration>()

   override val terminatedInstances: Flux<PublisherConfiguration> = sink.asFlux()

   init {
      if (pollFrequency.isZero) {
         logger.warn { "Polling for expired sources has been disabled!" }
      } else {
         Flux.interval(pollFrequency, scheduler)
            .map {
               lastPingTimes.filter { (publisherConfig, lastPingTime) ->
                  val keepAlive = publisherConfig.keepAlive as HttpPollKeepAlive
                  lastPingTime.isBefore(Instant.now().minus(keepAlive.pollFrequency))
               }
            }
            .flatMap { servicesToPing ->

               val pingResponses = servicesToPing.map { (publisherConfig, _) ->
                  performKeepAliveCheck(publisherConfig)
               }
               Flux.concat(pingResponses)
//               lastPingTimes.forEach { (publisherConfig, lastTimeStamp) ->
//                  val now = Instant.now()
//                  (publisherConfig.keepAlive as HttpPollKeepAlive).run {
//                     if (lastTimeStamp.isBefore(now.minus(this.pollFrequency)))
//                     // set the lastUpdated time for the entry
//                        lastPingTimes[publisherConfig] = Instant.MAX
//                     logger.info { "Performing keep alive check for $publisherConfig" }
//
//                  }
//               }
            }
            .subscribe()
      }
   }

   private fun performKeepAliveCheck(publisherConfig: PublisherConfiguration): Mono<Pair<PublisherConfiguration, Boolean>> {
      val keepAlive = publisherConfig.keepAlive as HttpPollKeepAlive
      return webClientBuilder
         .build()
         .get()
         .uri(keepAlive.pollUrl)
         .retrieve()
         .toBodilessEntity()
         .timeout(httpRequestTimeout)
         .map { entity ->
            if (!entity.statusCode.is2xxSuccessful) {
               logger.warn { "Keep alive call for $publisherConfig returned ${entity.statusCode} so marking the publisher as zombie" }
               lastPingTimes.remove(publisherConfig)?.let { _ ->
                  sink.emitNext(publisherConfig, RetryFailOnSerializeEmitHandler)
               }
               publisherConfig to false
            } else {
               lastPingTimes[publisherConfig] = Instant.now()
               publisherConfig to true
            }
         }
         .doOnError { error ->
            logger.error(error) {
               "Keep alive call for $publisherConfig returned Error so marking the publisher as zombie"
            }
            lastPingTimes.remove(publisherConfig)?.let { _ ->
               sink.emitNext(publisherConfig, RetryFailOnSerializeEmitHandler)
            }
         }
   }

   override fun appliesTo(keepAlive: KeepAliveStrategy) = keepAlive is HttpPollKeepAlive

   override fun monitor(publisherConfiguration: PublisherConfiguration) {
      logger.debug { "Monitoring keep alive for $publisherConfiguration" }
      lastPingTimes[publisherConfiguration] = Instant.now()
   }
}

object RetryFailOnSerializeEmitHandler : Sinks.EmitFailureHandler {
   override fun onEmitFailure(signalType: SignalType, emitResult: Sinks.EmitResult) =
      emitResult == Sinks.EmitResult.FAIL_NON_SERIALIZED
}

