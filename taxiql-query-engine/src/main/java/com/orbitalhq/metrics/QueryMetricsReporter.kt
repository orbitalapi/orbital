package com.orbitalhq.metrics

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.MetricTags
import com.orbitalhq.query.TypedInstanceWithMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

interface QueryMetricsReporter {
   fun invoked(tags: MetricTags)
   fun resultEmitted(tags: MetricTags)
   fun firstResult(duration: Duration, tags: MetricTags)
   fun completed(duration: Duration, finalCount: Int, tags: MetricTags)
   fun failed(duration: Duration, tags: MetricTags)

   fun observeQueryResult(
      request: Publisher<Any>,
      startTime: Instant,
      metricsTags: MetricTags,
      logDurationsOfIndividualMessages: Boolean
   ):Publisher<Any> {
      return when (request) {
         is Mono<*> -> observeRequestResponse(request as Mono<Any>,startTime,metricsTags)
         is Flux<*> -> observeResultFlux(request as Flux<Any>, startTime, metricsTags, logDurationsOfIndividualMessages)
         else -> error("Unexpected publisher type: ${request::class.simpleName}")
      }
   }
   /**
    * Records metrics on queries routed through the query router (ie., saved queries).
    * At present, we're
    */
   fun observeRequestResponse(
      request: Mono<Any>,
      startTime: Instant,
      metricsTags: MetricTags,
   ): Mono<Any> {
      if (metricsTags == MetricTags.NONE) {
         return request
      }

      val isFirst = AtomicBoolean(true)
      val counter = AtomicInteger(0)
      return request
         .doOnSubscribe { invoked(metricsTags) }
         .doOnEach {
            val wasFirst = isFirst.getAndSet(false)
            if (wasFirst) {
               firstResult(Duration.between(startTime, Instant.now()), metricsTags)
            }
            counter.incrementAndGet()
         }
         .doFinally {
            completed(Duration.between(startTime, Instant.now()), counter.get(), metricsTags)
         }
   }

   /**
    * Captures metrics for the result stream,
    * Unless the metrics tags provided are MetricsTags.NONE, in which case we don't bother.
    * This is so that Internal / nested queries don't capture metrics, as it skews the results.
    */
   fun observeEventStream(
      resultsWithMetadata: Flow<TypedInstanceWithMetadata>,
      startTime: Instant,
      metricsTags: MetricTags,
      logDurationsOfIndividualMessages: Boolean
   ): Flow<TypedInstance> {
      if (metricsTags == MetricTags.NONE) {
         return resultsWithMetadata.map { it.instance }
      }

      val isFirst = AtomicBoolean(true)
      val counter = AtomicInteger(0)
      return resultsWithMetadata
         .onStart { invoked(metricsTags) }
         .onEach {
            val wasFirst = isFirst.getAndSet(false)
            if (wasFirst) {
               firstResult(Duration.between(it.processingStart, Instant.now()), metricsTags)
            }
            if (logDurationsOfIndividualMessages) {
               completed(Duration.between(it.processingStart, Instant.now()), counter.get(), metricsTags)
            }
            resultEmitted(metricsTags)
            counter.incrementAndGet()
         }.onCompletion {
            completed(Duration.between(startTime, Instant.now()), counter.get(), metricsTags)
         }
         .map { it.instance }
   }

   fun observeResultFlux(
      resultsWithMetadata: Flux<Any>,
      startTime: Instant,
      metricsTags: MetricTags,
      logDurationsOfIndividualMessages: Boolean
   ): Flux<Any> {

      // We could be passed:
      // - A Flux<TypedInstance>
      // - A Flux<TypedInstanceWithMetadata> (for streaming queries)
      // - A raw value
      // We always want to return the raw value
      fun getRawResultFromFluxEvent(message:Any):Any? {
         return when (message) {
            is TypedInstanceWithMetadata -> message.instance.toRawObject()
            is TypedInstance -> message.toRawObject()
            else -> message
         }
      }
      if (metricsTags == MetricTags.NONE) {
         return resultsWithMetadata.mapNotNull { getRawResultFromFluxEvent(it) }
      }

      val isFirst = AtomicBoolean(true)
      val counter = AtomicInteger(0)
      return resultsWithMetadata
         .doOnSubscribe { invoked(metricsTags) }
         .doOnNext {
            val instanceStartTime = when {
               it is TypedInstanceWithMetadata -> it.processingStart
               else -> startTime
            }
            val wasFirst = isFirst.getAndSet(false)
            if (wasFirst) {
               firstResult(Duration.between(instanceStartTime, Instant.now()), metricsTags)
            }
            if (logDurationsOfIndividualMessages) {
               completed(Duration.between(instanceStartTime, Instant.now()), counter.get(), metricsTags)
            }
            resultEmitted(metricsTags)
            counter.incrementAndGet()
         }.doFinally {
            completed(Duration.between(startTime, Instant.now()), counter.get(), metricsTags)
         }
         .mapNotNull { getRawResultFromFluxEvent(it) }
   }
}

object NoOpMetricsReporter : QueryMetricsReporter {
   override fun invoked(tags: MetricTags) {
   }

   override fun resultEmitted(tags: MetricTags) {
   }

   override fun firstResult(duration: Duration, tags: MetricTags) {
   }

   override fun completed(duration: Duration, finalCount: Int, tags: MetricTags) {
   }

   override fun failed(duration: Duration, tags: MetricTags) {
   }
}


