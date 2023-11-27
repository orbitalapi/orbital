package com.orbitalhq.cockpit.core.monitoring

data class DataMetricSpec(
    val title: String,
    val unitLabel: String,
    val yAxisUnit: YAxisUnit,
    val promQlQuery: (queryStreamName: String, stepSize: String) -> String
)

object DataMetricSpecs {
    val messagesReceived = DataMetricSpec(
        "Messages Received",
        "msgs / sec",
        YAxisUnit.Count,
    ) { queryStreamName, stepSize -> """rate(orbital_pipelines_received_items_total{queryStream="$queryStreamName"}[$stepSize])""" }


    val averageQueryDuration = DataMetricSpec(
        "Average Duration",
        "ms",
        YAxisUnit.DurationInSecondsConvertToMillis
    )
    { queryStreamName, stepSize ->
        """sum(rate(orbital_query_duration_seconds_sum{queryStream="$queryStreamName"}[$stepSize])) / sum(rate(orbital_query_duration_seconds_count{queryStream="$queryStreamName"}[$stepSize]))"""
    }

    val maxQueryDuration =
        DataMetricSpec("Max Duration", "ms", YAxisUnit.DurationInSecondsConvertToMillis) { queryStreamName, stepSize ->
            """max_over_time(orbital_query_duration_seconds_max{queryStream="$queryStreamName"}[$stepSize])"""
        }

    val failures = DataMetricSpec(
        "Failures",
        "Count",
        YAxisUnit.Count
    ) { queryStreamName, stepSize -> """rate(orbital_query_failures_total{queryStream="$queryStreamName"}[$stepSize])""" }


    val queryInvocations = DataMetricSpec(
        "Requests",
        "req / sec",
        YAxisUnit.Count,
    ) { queryStreamName, stepSize -> """rate(orbital_query_invocations_total{queryStream="$queryStreamName"}[$stepSize])""" }
}
