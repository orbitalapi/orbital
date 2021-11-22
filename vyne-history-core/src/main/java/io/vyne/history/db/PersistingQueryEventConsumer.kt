package io.vyne.history.db

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.history.HistoryPersistenceQueue
import io.vyne.history.QueryAnalyticsConfig
import io.vyne.models.OperationResult
import io.vyne.models.json.Jackson
import io.vyne.query.QueryCompletedEvent
import io.vyne.query.QueryEvent
import io.vyne.query.QueryEventConsumer
import io.vyne.query.QueryFailureEvent
import io.vyne.query.QueryResponse
import io.vyne.query.QueryStartEvent
import io.vyne.query.RemoteCallOperationResultHandler
import io.vyne.query.RestfulQueryExceptionEvent
import io.vyne.query.RestfulQueryResultEvent
import io.vyne.query.TaxiQlQueryExceptionEvent
import io.vyne.query.TaxiQlQueryResultEvent
import io.vyne.query.history.QuerySummary
import io.vyne.history.QueryResultEventMapper
import io.vyne.history.QuerySummaryPersister
import io.vyne.query.VyneQueryStatisticsEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger {}
/**
 * A QueryEventConsumer which streams events out to be persisted.
 * QueryEvents have high degrees of overlap in terms of the entities they create.
 * (eg., Lineage / Data Sources for many events are the same).
 *
 * To reduce the number of trips to the db, we hold state of persisted keys of
 * many entities.
 *
 * Therefore, this object should be relatively short-lived (ie., for a single query)
 * to prevent memory leaks.
 */
class PersistingQueryEventConsumer(
   private val queryId: String,
   private val queryHistoryDao: QueryHistoryDao,
   private val persistenceQueue: HistoryPersistenceQueue,
   private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
   config: QueryAnalyticsConfig,
   private val scope: CoroutineScope

) : QuerySummaryPersister(queryHistoryDao), QueryEventConsumer, RemoteCallOperationResultHandler {
   val lastWriteTime = AtomicLong(System.currentTimeMillis())
   private val sankeyViewBuilder = LineageSankeyViewBuilder()
   private val resultRowPersistenceStrategy = ResultRowPersistenceStrategyFactory.resultRowPersistenceStrategy(objectMapper, persistenceQueue, config)
   /**
    * Shutdown subscription to query history queue and clear down the queue files
    */
   fun shutDown() {
      logger.info { "Query result handler shutting down - $queryId" }
      if (!sankeyChartPersisted) {
         queryHistoryDao.persistSankeyChart(queryId, sankeyViewBuilder)
      }
   }


   override fun handleEvent(event: QueryEvent) {
      scope.launch {
         lastWriteTime.set(System.currentTimeMillis())
         when (event) {
            is TaxiQlQueryResultEvent -> persistEvent(event)
            is RestfulQueryResultEvent -> persistEvent(event)
            is QueryCompletedEvent -> persistEvent(event, sankeyViewBuilder)
            is TaxiQlQueryExceptionEvent -> persistEvent(event)
            is QueryFailureEvent -> persistEvent(event)
            is RestfulQueryExceptionEvent -> persistEvent(event)
            is QueryStartEvent -> persistEvent(event)
            is VyneQueryStatisticsEvent -> {}
         }
      }
   }

   private fun persistEvent(event: QueryStartEvent) {
      logger.info { "Recording that query ${event.queryId} has started" }

      createQuerySummaryRecord(event.queryId) {
         QuerySummary(
            queryId = event.queryId,
            clientQueryId = event.clientQueryId,
            taxiQl = event.taxiQuery,
            queryJson = event.query?.let { objectMapper.writeValueAsString(event.query)  } ,
            responseStatus = QueryResponse.ResponseStatus.RUNNING,
            startTime = event.timestamp,
            responseType = event.message
         )
      }
   }

   private fun persistEvent(event: RestfulQueryResultEvent) {
      createQuerySummaryRecord(event.queryId) {
         QueryResultEventMapper.toQuerySummary(event)
      }

      resultRowPersistenceStrategy.persistResultRowAndLineage(event)
      appendToSankeyChart(event.typedInstance, this.sankeyViewBuilder)
   }

   private fun persistEvent(event: TaxiQlQueryResultEvent) {
      createQuerySummaryRecord(event.queryId) {
         try {
            QueryResultEventMapper.toQuerySummary(event)
         } catch (e: Exception) {
            throw e
         }
      }
      resultRowPersistenceStrategy.persistResultRowAndLineage(event)
      appendToSankeyChart(event.typedInstance, this.sankeyViewBuilder)
   }

   override fun recordResult(operation: OperationResult, queryId: String) {
      // Here, we're writing the operation invocations.
      // These can also be persisted during persistence of the result record.
      // However, Traversing all the OperationResult entries to get the
      // grandparent operation results from parameters is quite tricky.
      // Instead, we're capturing them out-of-band.
      val lineageRecords = resultRowPersistenceStrategy.createLineageRecords(listOf(operation), queryId)
      lineageRecords.forEach { persistenceQueue.storeLineageRecord(it) }
      appendOperationResultToSankeyChart(operation, this.sankeyViewBuilder)
   }

   fun finalize() {
      logger.debug { "PersistingQueryEventConsumer being finalized for query id $queryId now" }
   }

}
