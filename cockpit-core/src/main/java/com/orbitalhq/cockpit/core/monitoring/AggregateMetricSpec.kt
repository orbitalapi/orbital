package com.orbitalhq.cockpit.core.monitoring

data class AggregateMetricSpec(
   val title: String,
   val unitLabel: String,
   val yAxisUnit: YAxisUnit,
   val promQlQuery: (stepSize: String) -> String
)

object AggregateMetricSpecs {
    val messagesReceived = AggregateMetricSpec(
        "Messages Received",
        "msgs / sec",
        YAxisUnit.Count,
    ) { stepSize -> """sum by (queryStream) (
    rate(orbital_query_invocations_total{queryStream=~".*"}[$stepSize])
  + rate(orbital_pipelines_received_items_total{queryStream=~".*"}[$stepSize])
)""" }

    val averageQueryDuration = AggregateMetricSpec(
        "Average Duration",
        "ms",
        YAxisUnit.DurationInSecondsConvertToMillis
    )
    { stepSize ->
        """sum(rate(orbital_query_duration_seconds_sum{queryStream=~".*"}[$stepSize])) / sum(rate(orbital_query_duration_seconds_count{queryStream=~".*"}[$stepSize]))"""
    }

    val maxQueryDuration =
       AggregateMetricSpec("Max Duration", "ms", YAxisUnit.DurationInSecondsConvertToMillis) { stepSize ->
            """max_over_time(orbital_query_duration_seconds_max{queryStream=~".*"}[$stepSize])"""
        }

    val failures = AggregateMetricSpec(
        "Failures",
        "Count",
        YAxisUnit.Count
    ) { stepSize -> """rate(orbital_query_failures_total{queryStream=~".*"}[$stepSize])""" }
}
