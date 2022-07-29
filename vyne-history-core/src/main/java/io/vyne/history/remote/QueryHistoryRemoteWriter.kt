package io.vyne.history.remote

import io.vyne.history.QueryAnalyticsConfig
import io.vyne.history.db.LineageSankeyViewBuilder
import io.vyne.history.db.ResultRowPersistenceStrategyFactory
import io.vyne.models.OperationResult
import io.vyne.models.json.Jackson
import io.vyne.query.HistoryEventConsumerProvider
import io.vyne.query.QueryCompletedEvent
import io.vyne.query.QueryEvent
import io.vyne.query.QueryEventConsumer
import io.vyne.query.RestfulQueryResultEvent
import io.vyne.query.TaxiQlQueryResultEvent
import io.vyne.schemas.Schema
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import mu.KotlinLogging
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.http.MediaType
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger {}
class QueryHistoryRemoteWriter(private val config: QueryAnalyticsConfig,
                               rsocketStrategies: RSocketStrategies,
                               private val discoveryClient: DiscoveryClient): HistoryEventConsumerProvider {
   private val historyDispatcher = Executors
      .newFixedThreadPool(1)
      .asCoroutineDispatcher()
   private val queryEventConsumer = RemoteQueryEventConsumerClient(
      ResultRowPersistenceStrategyFactory.resultRowPersistenceStrategy(Jackson.defaultObjectMapper, null, config),
      CoroutineScope(historyDispatcher)
   )

   init {
      val responder =
         RSocketMessageHandler.responder(rsocketStrategies, queryEventConsumer)

      resolveHistoryServerHostPort()
         .flatMap { (host, port) ->
         RSocketRequester
            .builder()
            .dataMimeType(MediaType.APPLICATION_CBOR)
            .rsocketStrategies(rsocketStrategies)
            .rsocketConnector { connector ->
               connector
                  .acceptor(responder)
            }
            .
            connectTcp(host, port)
            .flatMap { r ->
               r.rsocket()
                  .onClose()
                  .doOnSubscribe {
                  logger.info { "Subscribed to OnClose" }
               }
            }
      }
         .repeat()
         .retryWhen(Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(10))
            .doBeforeRetry { retrySignal: Retry.RetrySignal? ->
               logger.warn {"Connection Closed re-trying {$retrySignal}"}
            })
         .doOnError { logger.warn {
            "Vyne Query History Service   connection is closed!" } }
         .doFinally { logger.warn {
            "Vyne Query History Service   connection is disconnected!" } }
         .subscribe()
   }

   private fun resolveHistoryServerHostPort(): Mono<Pair<String, Int>> {
     return Mono.defer {
         val instances = discoveryClient.getInstances(config.analyticsServerApplicationName)
         if (instances.isEmpty()) {
            return@defer Mono.error(IllegalStateException("Can't find any ${config.analyticsServerApplicationName} instance registered on Eureka"))
         }
         // TODO : Consider round-robin with Ribbon, or to randomize
         val firstHistoryServiceInstance = instances.first()
         val historyServiceHost = firstHistoryServiceInstance.host
         val historyServicePort = firstHistoryServiceInstance.metadata["vyne-analytics-port"]
            ?: return@defer Mono.error(IllegalStateException("Can't find any vyne-analytics-server instance registered on Eureka"))
         return@defer Mono.just(Pair(historyServiceHost, historyServicePort.toInt()))
      }


   }
   override fun createEventConsumer(queryId: String, schema: Schema): QueryEventConsumer {
      return RemoteDelegatingQueryEventConsumer(queryEventConsumer, queryId, schema)
   }
}

class RemoteDelegatingQueryEventConsumer(private val queryEventConsumer: RemoteQueryEventConsumerClient, private val queryId: String, private val schema:Schema): QueryEventConsumer {
   @Volatile
   private var sankeyChartPersisted: Boolean = false

   val lastWriteTime = AtomicLong(System.currentTimeMillis())
   private val sankeyViewBuilder = LineageSankeyViewBuilder(schema)
   override fun handleEvent(event: QueryEvent) {
      queryEventConsumer.handleEvent(event)
      when(event) {
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
