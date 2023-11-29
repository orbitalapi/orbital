package com.orbitalhq.cockpit.core.monitoring

import com.orbitalhq.http.ServicesConfig
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.SavedQuery
import com.orbitalhq.schemas.taxi.asSavedQuery
import com.orbitalhq.spring.config.LoadBalancerFilterFunction
import com.orbitalhq.spring.http.NotFoundException
import lang.taxi.query.TaxiQlQuery
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Returns performance metrics for... things (connections, queries, streams, pipelines, etc)
 *
 */
@RestController
class TelemetryService(
    private val schemaProvider: SchemaProvider,
    webClientBuilder: WebClient.Builder,
    discoveryClient: DiscoveryClient
) {

    private val webClient =
        webClientBuilder.clone().filter(LoadBalancerFilterFunction(discoveryClient))
            .build()

    private val streamDataMetricSpecs = listOf(
        DataMetricSpecs.messagesReceived,
        DataMetricSpecs.averageQueryDuration,
        DataMetricSpecs.maxQueryDuration,
        DataMetricSpecs.failures
    )


    private val queryDataMetricSpecs = listOf(
        DataMetricSpecs.queryInvocations,
        DataMetricSpecs.averageQueryDuration,
        DataMetricSpecs.maxQueryDuration,
        DataMetricSpecs.failures
    )


    @GetMapping("/api/metrics/stream/{name}")
    fun getMetricsForStream(
        @PathVariable("name") qualifiedName: String,
        @RequestParam(name = "period", required = false, defaultValue = "Last4Hours") period: MetricsWindow
    ): Mono<StreamMetricsData> {
        val query = try {
            schemaProvider.schema
                .taxi.query(qualifiedName)
        } catch (e: Exception) {
            throw NotFoundException("No query named $qualifiedName is present in this schema")
        }
        return when (query.asSavedQuery().queryKind) {
            SavedQuery.QueryKind.Query -> buildStreamMetrics(query, period, queryDataMetricSpecs)
            SavedQuery.QueryKind.Stream -> buildStreamMetrics(query, period, streamDataMetricSpecs)
        }
    }

    private fun buildStreamMetrics(query: TaxiQlQuery, window: MetricsWindow, metricsSpecs: List<DataMetricSpec>): Mono<StreamMetricsData> {
        val endTime = Instant.now()
        val startTime = endTime.minus(window.duration)
        val stepSize = "30s"
        val httpRequests: List<Mono<Pair<DataMetricSpec, RawSeriesData>>> = metricsSpecs.map { spec ->
            loadDataSeries(startTime, endTime, spec.promQlQuery(query.name.fullyQualifiedName, stepSize), stepSize)
                .map { spec to it }
        }
        return Flux.merge(httpRequests)
            .collectList()
            .map { specsAndResults: List<Pair<DataMetricSpec, RawSeriesData>> ->

                // Sorting is important so that the charts appear in a consistent order on the UI
                val dataSeries = specsAndResults.sortedBy { (spec, _) -> streamDataMetricSpecs.indexOf(spec) }
                    .map { (spec, data) ->
                        DataSeries(spec.title, spec.unitLabel, spec.yAxisUnit, data.series)
                    }
                val tags = specsAndResults.firstOrNull()?.second?.tags ?: emptyMap()
                StreamMetricsData(tags, dataSeries)
            }
    }

    private fun loadDataSeries(
        startTime: Instant,
        endTime: Instant,
        promQlQuery: String,
        stepSize: String
    ): Mono<RawSeriesData> {
        // Hard-learnt lesson: Don't try to use spring's URI builder, as it gets thrown out by the {} symbols within the PromQL query
        val uri = "http://${ServicesConfig.METRICS_SERVER_NAME}/api/v1/query_range?query={query}&start={start}&end={end}&step={step}"

        return webClient.get().uri(
            uri,
            mapOf(
                "query" to promQlQuery,
                "start" to startTime.toIsoString(),
                "end" to endTime.toIsoString(),
                "step" to stepSize
            )
        ).retrieve().bodyToMono<PrometheusQueryRangeMetricsResult>()
            .map { prometheusMetrics ->
                val resultItem = prometheusMetrics.data.result.firstOrNull()
                    ?: return@map RawSeriesData.empty
                val tags = resultItem.metric
                val metrics = resultItem.valuesAsTimestampedValues
                RawSeriesData(tags, metrics)
            }
    }

    private fun buildQueryMetrics(query: TaxiQlQuery): Mono<StreamMetricsData> {
        TODO("Not yet implemented")
    }
}

enum class MetricsWindow(val duration: Duration) {
    Last30Seconds(Duration.ofSeconds(30)),
    LastMinute(Duration.ofMinutes(1)),
    Last5Minutes(Duration.ofMinutes(5)),
    LastHour(Duration.ofMinutes(60)),
    Last4Hours(Duration.ofHours(4)),
    LastDay(Duration.ofDays(1)),
    Last7Days(Duration.ofDays(7)),
    Last30Days(Duration.ofDays(30)),

}


fun Instant.toIsoString() = DateTimeFormatter.ISO_INSTANT.format(this)
