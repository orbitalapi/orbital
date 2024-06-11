package com.orbitalhq.cockpit.core.monitoring

import java.time.Instant

/**
 * The key-value metrics emitted by Micrometer when we capture pipeline metrics
 */
data class PipelineTags(
   val application: String,
   val instance: String,
   val job: String,
   val pipeline: String
)

enum class YAxisUnit {
   Count,
   DurationInSeconds,
   DurationInSecondsConvertToMillis
}

data class DataSeries(
   val title: String,
   val unitLabel: String,
   val unit: YAxisUnit,
   val series: List<MetricTimestampValue>
) {

}

data class MetricTimestampValue(
   val timestamp: Instant,
   val epochSeconds: Long,
   val value: Any
)

data class StreamMetricsData(

   val tags: Map<String, Any>,
   val series: List<DataSeries>,
   val metricsAvailable: Boolean = true,
   val errorMessage: String? = null
) {
   companion object {
      val empty = StreamMetricsData(emptyMap(), emptyList(), true)
      fun unavailable(message: String? = null) = StreamMetricsData(emptyMap(), emptyList(), false, message)
   }
}

data class RawSeriesData(val tags: Map<String, Any>, val series: List<MetricTimestampValue>) {
   companion object {
      val empty = RawSeriesData(emptyMap(), emptyList())
   }
}
