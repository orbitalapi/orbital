package com.orbitalhq.spring.metrics

import com.orbitalhq.metrics.QueryMetricsReporter
import com.orbitalhq.query.MetricTags
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import java.time.Duration

fun MetricTags.micrometerTags(): List<Tag> {
   return this.tags.map { (k, v) -> Tag.of(k, v) }
}

class MicrometerMetricsReporter(private val meterRegistry: MeterRegistry) : QueryMetricsReporter {
   override fun invoked(tags: MetricTags) {
      if (tags == MetricTags.NONE) return
      meterRegistry.counter("orbital.query.invocations", tags.micrometerTags())
         .increment()
   }

   override fun resultEmitted(tags: MetricTags) {
      if (tags == MetricTags.NONE) return
      meterRegistry.counter("orbital.query.resultEmitted", tags.micrometerTags())
         .increment()
   }

   override fun firstResult(duration: Duration, tags: MetricTags) {
      if (tags == MetricTags.NONE) return
      meterRegistry.timer("orbital.query.firstResultTime", tags.micrometerTags())
         .record(duration)
   }

   override fun completed(duration: Duration, finalCount: Int, tags: MetricTags) {
      if (tags == MetricTags.NONE) return
      meterRegistry.timer("orbital.query.duration", tags.micrometerTags())
         .record(duration)
   }

   override fun failed(duration: Duration, tags: MetricTags) {
      if (tags == MetricTags.NONE) return
      meterRegistry.counter("orbital.query.failures", tags.micrometerTags())
         .increment()
   }

}
