package io.vyne.history.remote

import io.vyne.history.QueryAnalyticsConfig
import io.vyne.history.ResultRowPersistenceStrategyFactory
import io.vyne.history.chart.LineageSankeyViewBuilder
import io.vyne.models.OperationResult
import io.vyne.models.json.Jackson
import io.vyne.query.*
import io.vyne.schemas.Schema
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import mu.KotlinLogging
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.messaging.rsocket.RSocketStrategies
import reactor.core.Disposable
import java.util.concurrent.Executors

private val logger = KotlinLogging.logger {}

// Used within QueryService, where we have a static DiscoveryClient
class QueryHistoryRSocketWriter(
   config: QueryAnalyticsConfig,
   rsocketStrategies: RSocketStrategies,
   discoveryClient: DiscoveryClient
) : HistoryEventConsumerProvider, BaseHistoryRSocketWriter(config) {
   private val historyDispatcher = Executors
      .newFixedThreadPool(1)
      .asCoroutineDispatcher()
   private val queryEventConsumer = RemoteQueryEventConsumerClient(
      ResultRowPersistenceStrategyFactory.resultRowPersistenceStrategy(Jackson.defaultObjectMapper, null, config),
      config,
      CoroutineScope(historyDispatcher)
   )

   private val rsocketPublisherSubscription: Disposable =
      super.buildRSocketPublisher(rsocketStrategies, discoveryClient, queryEventConsumer)
         .subscribe()


   fun shutDown() {
      rsocketPublisherSubscription.dispose()
   }

   override fun createEventConsumer(queryId: String, schema: Schema): QueryEventConsumer {
      return RemoteDelegatingQueryEventConsumer(queryEventConsumer, queryId, schema)
   }
}

class RemoteDelegatingQueryEventConsumer(
   private val queryEventConsumer: RemoteQueryEventConsumerClient,
   private val queryId: String,
   private val schema: Schema
) : QueryEventConsumer {
   @Volatile
   private var sankeyChartPersisted: Boolean = false

   private val sankeyViewBuilder = LineageSankeyViewBuilder(schema)
   override fun handleEvent(event: QueryEvent) {
      queryEventConsumer.handleEvent(event)
      when (event) {
         is RestfulQueryResultEvent -> sankeyViewBuilder.append(event.typedInstance)
         is TaxiQlQueryResultEvent -> sankeyViewBuilder.append(event.typedInstance)
         is QueryCompletedEvent -> emitFlowChartData()
         else -> {}
      }
   }

   override fun recordResult(operation: OperationResult, queryId: String) {
      queryEventConsumer.recordResult(operation, queryId)
      sankeyViewBuilder.captureOperationResult(operation)
   }

   private fun emitFlowChartData() {
      if (!sankeyChartPersisted) {
         val chartRows = sankeyViewBuilder.asChartRows(queryId)
         queryEventConsumer.pushSankeyData(chartRows, queryId)
         sankeyChartPersisted = true
      }
   }

   fun shutDown() {
      logger.info { "Query result handler shutting down - $queryId" }
      emitFlowChartData()
   }
}
