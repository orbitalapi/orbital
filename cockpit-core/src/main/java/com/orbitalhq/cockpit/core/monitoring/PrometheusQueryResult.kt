package com.orbitalhq.cockpit.core.monitoring

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant

data class PrometheusQueryRangeMetricsResult(
    val status: String,
    val data: PrometheusData
)

data class PrometheusData(
    val resultType: String,
    val result: List<ResultItem>
)

data class ResultItem(
    val metric: Map<String,String>, // Tags
    val values: List<List<Any>> // Pair of timestamp and value
) {
    val valuesAsTimestampedValues: List<MetricTimestampValue> by lazy {
        this.values.map { values ->
            MetricTimestampValue(
                Instant.ofEpochSecond((values[0] as Double).toLong()),
                (values[0] as Double).toLong(),
                values[1]
            )
        }
    }
    inline fun <reified T> metricsAs():T {
        return mapper.convertValue<T>(metric)
    }

    companion object {
        val mapper = jacksonObjectMapper()
    }
}


