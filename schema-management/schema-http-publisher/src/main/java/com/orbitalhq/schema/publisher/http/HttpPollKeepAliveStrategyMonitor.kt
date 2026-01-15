package com.orbitalhq.schema.publisher.http

import com.orbitalhq.schema.publisher.*
import com.orbitalhq.utils.RetryFailOnSerializeEmitHandler
import mu.KotlinLogging
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
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
   private val restClientBuilder: RestClient.Builder = RestClient.builder()
) : KeepAliveStrategyMonitor {
   private val sink = Sinks.many().multicast()
      .onBackpressureBuffer<PublisherHealthUpdateMessage>()

   override val healthUpdateMessages: Flux<PublisherHealthUpdateMessage> = sink.asFlux()
   private val restClient = restClientBuilder.requestFactory(
      SimpleClientHttpRequestFactory().apply {
         setConnectTimeout(httpRequestTimeout)
         setReadTimeout(httpRequestTimeout)
      }
   ).build()

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
                  Mono.fromCallable {
                     performKeepAliveCheck(publisherConfig)
                  }
               }
               Flux.concat(pingResponses)
            }
            .subscribe()
      }
   }
   private fun performKeepAliveCheck(publisherConfig: PublisherConfiguration): Pair<PublisherConfiguration, Boolean> {
      val keepAlive = publisherConfig.keepAlive as HttpPollKeepAlive

      return try {
         val response = restClient
            .get()
            .uri(keepAlive.pollUrl)
            .retrieve()
            .toBodilessEntity()

         if (!response.statusCode.is2xxSuccessful) {
            logger.warn { "Keep alive call for $publisherConfig returned ${response.statusCode} so marking the publisher as unhealthy" }
            lastPingTimes.remove(publisherConfig)?.let { _ ->
               sink.emitNext(
                  PublisherHealthUpdateMessage(
                     publisherConfig.publisherId,
                     PublisherHealth(
                        PublisherHealth.Status.Unhealthy,
                        "Keep alive call returned ${response.statusCode}"
                     )
                  ),
                  RetryFailOnSerializeEmitHandler
               )
            }
            publisherConfig to false
         } else {
            lastPingTimes[publisherConfig] = Instant.now()
            publisherConfig to true
         }
      } catch (error: Exception) {
         logger.error(error) {
            "Keep alive call for $publisherConfig returned Error so marking the publisher as unhealthy"
         }
         lastPingTimes.remove(publisherConfig)?.let { _ ->
            sink.emitNext(
               PublisherHealthUpdateMessage(
                  publisherConfig.publisherId,
                  PublisherHealth(
                     PublisherHealth.Status.Unhealthy,
                     "Keep alive call returned error: ${error.message}"
                  )
               ),
               RetryFailOnSerializeEmitHandler
            )
         }
         publisherConfig to false
      }
   }
   override fun appliesTo(keepAlive: KeepAliveStrategy) = keepAlive is HttpPollKeepAlive

   override fun monitor(publisherConfiguration: PublisherConfiguration) {
      logger.debug { "Monitoring keep alive for $publisherConfiguration" }
      lastPingTimes[publisherConfiguration] = Instant.now()
   }
}


