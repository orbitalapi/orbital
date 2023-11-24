package com.orbitalhq.cockpit.core.monitoring

data class DataMetricSpec(
   val title: String,
   val unitLabel: String,
   val yAxisUnit:  YAxisUnit,
   val promQlQuery: (pipelineName: String, stepSize: String) -> String
)

object DataMetricSpecs {
   val messagesReceived = DataMetricSpec(
      "Messages Received",
      "msgs / sec",
      YAxisUnit.Count,
   ) { pipelineName, stepSize -> """rate(orbital_pipelines_received_items_total{pipeline="$pipelineName"}[$stepSize])""" }


   val averageQueryDuration = DataMetricSpec("Average Duration", "ms", YAxisUnit.DurationInSecondsConvertToMillis) { pipelineName, stepSize ->
      """sum(rate(orbital_query_duration_seconds_sum{pipeline="$pipelineName"}[$stepSize])) /   sum(rate(orbital_query_duration_seconds_count{pipeline="$pipelineName"}[$stepSize]))"""
   }

   val maxQueryDuration = DataMetricSpec("Max Duration", "ms", YAxisUnit.DurationInSecondsConvertToMillis) { pipelineName, stepSize ->
      """max_over_time(orbital_query_duration_seconds_max{pipeline="$pipelineName"}[$stepSize])"""
   }

   val failures = DataMetricSpec(
      "Failures",
      "Count",
      YAxisUnit.Count
   ) { pipelineName, stepSize -> """rate(orbital_query_failures_total{pipeline="$pipelineName"}[$stepSize])""" }
}
