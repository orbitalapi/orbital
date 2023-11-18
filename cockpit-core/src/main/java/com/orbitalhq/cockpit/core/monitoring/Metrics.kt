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

data class MetricTimestampValue(
    val timestamp: Instant,
    val epochSeconds: Long,
    val value: Any
)

data class StreamMetricsData(
    val tags: PipelineTags,
    val metrics: List<MetricTimestampValue>
)
