package com.orbitalhq.metrics

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.MetricTags
import com.orbitalhq.query.TypedInstanceWithMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

interface QueryMetricsReporter {
   fun invoked(tags: MetricTags)
   fun resultEmitted(tags: MetricTags)
   fun firstResult(duration: Duration, tags: MetricTags)
   fun completed(duration: Duration, finalCount: Int, tags: MetricTags)
   fun failed(duration:Duration, tags: MetricTags)

   /**
    * Records metrics on queries routed through the query router (ie., saved queries).
    * At present, we're
    */
   fun observeRequestResponse(
      flux: Flux<Any>,
      startTime: Instant,
      metricsTags: MetricTags,
   ): Flux<Any> {
      if (metricsTags == MetricTags.NONE) {
         return flux
      }

      val isFirst = AtomicBoolean(true)
      val counter = AtomicInteger(0)
      return flux
         .doOnSubscribe { invoked(metricsTags) }
         .doOnEach {
            val wasFirst = isFirst.getAndSet(false)
            if (wasFirst) {
               firstResult(Duration.between(startTime, Instant.now()), metricsTags)
            }
            counter.incrementAndGet()
         }
         .doOnComplete {
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
   ):Flow<TypedInstance> {
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
