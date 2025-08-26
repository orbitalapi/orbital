package com.orbitalhq.cockpit.core.monitoring

import com.orbitalhq.http.ServicesConfig
import com.orbitalhq.pipelines.jet.api.streams.StreamUtils
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.SavedQuery
import com.orbitalhq.schemas.taxi.asSavedQuery
import com.orbitalhq.schemas.toVyneQualifiedName
import com.orbitalhq.security.VynePrivileges
import com.orbitalhq.spring.config.LoadBalancerFilterFunction
import com.orbitalhq.spring.http.NotFoundException
import lang.taxi.query.TaxiQlQuery
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
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

   private val aggregateMetricSpecs = listOf(
      AggregateMetricSpecs.messagesReceived,
      AggregateMetricSpecs.averageQueryDuration,
      AggregateMetricSpecs.maxQueryDuration,
      //AggregateMetricSpecs.failures
   )

   private val streamDataMetricSpecs = listOf(
      DataMetricSpecs.messagesReceived,
      DataMetricSpecs.averageQueryDuration,
      DataMetricSpecs.maxQueryDuration,
      //DataMetricSpecs.failures
   )

   private val queryDataMetricSpecs = listOf(
      DataMetricSpecs.queryInvocations,
      DataMetricSpecs.averageQueryDuration,
      DataMetricSpecs.maxQueryDuration,
      //DataMetricSpecs.failures
   )

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewMetrics}')")
   @GetMapping("/api/metrics/stream")
   fun getAggregateForAllStreams(
      @RequestParam(name = "period", required = false, defaultValue = "Last4Hours") period: MetricsWindow
   ): Mono<StreamMetricsData> {
      return buildStreamMetrics(period, aggregateMetricSpecs)
         .subscribeOn(Schedulers.boundedElastic())
   }


   @PreAuthorize("hasAuthority('${VynePrivileges.ViewMetrics}')")
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
         SavedQuery.QueryKind.Query -> buildEndpointMetrics(query, period, queryDataMetricSpecs)
         SavedQuery.QueryKind.Stream -> buildEndpointMetrics(query, period, streamDataMetricSpecs)
      }
   }

   private fun buildEndpointMetrics(
      query: TaxiQlQuery,
      window: MetricsWindow,
      metricsSpecs: List<DataMetricSpec>
   ): Mono<StreamMetricsData> {
      val dateRange = window.asRange()
      val stepSize = "30s"
      val httpRequests: List<Mono<Pair<PrometheusMetricSpec, Result<RawSeriesData>>>> = metricsSpecs.map { spec ->
         loadDataSeries(
            dateRange.start,
            dateRange.endInclusive,
            spec.promQlQuery(StreamUtils.toPipelineName(query.name.toVyneQualifiedName()), stepSize),
            stepSize
         )
            .map { spec to it }
      }
      return buildMetricsDataFromResults(httpRequests, metricsSpecs)
   }

   private fun buildStreamMetrics(
      window: MetricsWindow,
      metricsSpecs: List<AggregateMetricSpec>
   ): Mono<StreamMetricsData> {
      val range = window.asRange()
      val endTime = range.endInclusive
      val startTime = range.start
      val stepSize = "30s"
      val httpRequests: List<Mono<Pair<PrometheusMetricSpec, Result<RawSeriesData>>>> = metricsSpecs.map { spec ->
         loadDataSeries(startTime, endTime, spec.promQlQuery(stepSize), stepSize)
            .map { spec to it }
      }

      return buildMetricsDataFromResults(httpRequests, metricsSpecs)
   }

   private fun buildMetricsDataFromResults(
      httpRequests: List<Mono<Pair<PrometheusMetricSpec, Result<RawSeriesData>>>>,
      metricsSpecs: List<PrometheusMetricSpec>
   ): Mono<StreamMetricsData> {
      return Flux.merge(httpRequests)
         .collectList()
         .map { specsAndResults: List<Pair<PrometheusMetricSpec, Result<RawSeriesData>>> ->
            // Sorting is important so that the charts appear in a consistent order on the UI
            val (successes, failures) = specsAndResults.partition { it.second.isSuccess }
            val loadedMetrics = successes
               .map { (spec, result) -> spec to result.getOrNull()!! }
            if (successes.isEmpty()) {
               val errors = failures.mapNotNull { it.second.exceptionOrNull()!!.message }
                  .distinct()
               val errorMessage = "Loading metrics failed with the following errors: ${errors.joinToString(", ")}"
               StreamMetricsData.unavailable(errorMessage)
            } else {
               val dataSeries = loadedMetrics
                  .sortedBy { (spec, _) ->
                     metricsSpecs.indexOf(spec)
                  }
                  .mapIndexed { index, (spec, data) ->
                     DataSeries(spec.title, spec.unitLabel, spec.yAxisUnit, data.series)
                  }
               val tags = loadedMetrics.firstOrNull()?.second?.tags ?: emptyMap()
               StreamMetricsData(tags, dataSeries)
            }


         }
   }

   private fun loadDataSeries(
      startTime: Instant,
      endTime: Instant,
      promQlQuery: String,
      stepSize: String
   ): Mono<Result<RawSeriesData>> {
      // Hard-learnt lesson: Don't try to use spring's URI builder, as it gets thrown out by the {} symbols within the PromQL query
      val uri =
         "http://${ServicesConfig.METRICS_SERVER_NAME}/api/v1/query_range?query={query}&start={start}&end={end}&step={step}"

      val startTimeIso = startTime.toIsoString()
      val endTimeIso =  endTime.toIsoString()
      return webClient.get().uri(
         uri,
         mapOf(
            "query" to promQlQuery,
            "start" to startTimeIso,
            "end" to endTimeIso,
            "step" to stepSize
         )
      ).retrieve().bodyToMono<PrometheusQueryRangeMetricsResult>()
         .map { prometheusMetrics ->
            val resultItem = prometheusMetrics.data.result.firstOrNull()
               ?: return@map Result.success(RawSeriesData.empty)
            val tags = resultItem.metric
            val metrics = resultItem.valuesAsTimestampedValues
            Result.success(RawSeriesData(tags, metrics))
         }
         .onErrorResume { e -> Mono.just(Result.failure(e)) }
   }
}


fun Instant.toIsoString() = DateTimeFormatter.ISO_INSTANT.format(this)
