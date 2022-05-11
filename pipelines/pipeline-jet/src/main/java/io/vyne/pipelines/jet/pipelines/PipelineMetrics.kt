package io.vyne.pipelines.jet.pipelines

import com.hazelcast.jet.core.metrics.JobMetrics
import com.hazelcast.jet.core.metrics.Measurement
import io.vyne.pipelines.jet.api.MetricValue
import io.vyne.pipelines.jet.api.MetricValueSet
import io.vyne.pipelines.jet.api.PipelineMetrics
import java.time.Instant

 object MetricsHelper {
   private fun sumValues(measurements: List<Measurement>): Pair<Long, Instant> {
      val sum = measurements.sumOf { it.value() }
      val latestTimestamp = measurements.maxByOrNull { it.timestamp() }!!.timestamp()
      return sum to Instant.ofEpochMilli(latestTimestamp)
   }

   private fun latestValue(measurements: List<Measurement>): Pair<Long, Instant> {
      val latestMeasurement = measurements.maxByOrNull { it.timestamp() }!!
      return latestMeasurement.value() to Instant.ofEpochMilli(latestMeasurement.timestamp())
   }

   fun pipelineMetrics(metrics: JobMetrics): PipelineMetrics {
      val emittedCount = metricValueSet(metrics.get("emittedCount"), MetricsHelper::sumValues)
      val receivedCount = metricValueSet(metrics.get("receivedCount"), MetricsHelper::sumValues)
      val inflight = metricValueSet(metrics.get("receivedCount"), MetricsHelper::latestValue)
      val queueSize = metricValueSet(metrics.get("queuesSize"), MetricsHelper::latestValue)

      return PipelineMetrics(receivedCount, emittedCount, inflight, queueSize)
   }

   fun metricValueSet(
      measurements: List<Measurement>,
      latestValueProvider: (List<Measurement>) -> Pair<Long, Instant>
   ): List<MetricValueSet> {
      return measurements.groupBy { m -> m.tag("address") ?: "Unknown address" }
         .map { (address, addressMeasurements) ->
            val latestValue = if (addressMeasurements.isEmpty()) {
               null
            } else {
               val (latestValue, latestDate) = latestValueProvider.invoke(measurements.map { it })
               MetricValue(
                  latestValue,
                  latestDate
               )
            }
            val metricValues = addressMeasurements.map {
               MetricValue(
                  it.value(),
                  Instant.ofEpochMilli(it.timestamp())
               )
            }
            MetricValueSet(address, metricValues, latestValue)
         }

   }
}

