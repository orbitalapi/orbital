package com.orbitalhq.history.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.history.HistoryPersistenceQueue
import com.orbitalhq.history.QueryAnalyticsConfig
import com.orbitalhq.history.QueryResultEventMapper
import com.orbitalhq.history.ResultRowPersistenceStrategyFactory
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.json.Jackson
import com.orbitalhq.query.QueryCompletedEvent
import com.orbitalhq.query.QueryEvent
import com.orbitalhq.query.QueryEventConsumer
import com.orbitalhq.query.QueryFailureEvent
import com.orbitalhq.query.QueryStartEvent
import com.orbitalhq.query.RemoteCallOperationResultHandler
import com.orbitalhq.query.RestfulQueryExceptionEvent
import com.orbitalhq.query.RestfulQueryResultEvent
import com.orbitalhq.query.StreamingQueryCancelledEvent
import com.orbitalhq.query.TaxiQlQueryExceptionEvent
import com.orbitalhq.query.TaxiQlQueryResultEvent
import com.orbitalhq.query.history.RemoteCallResponse
import com.orbitalhq.schemas.Schema
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
   private val config: QueryAnalyticsConfig,
   private val scope: CoroutineScope,
   schema: Schema

) : QuerySummaryPersister(queryHistoryDao, queryId, schema), QueryEventConsumer, RemoteCallOperationResultHandler {
   val lastWriteTime = AtomicLong(System.currentTimeMillis())

   private val resultRowPersistenceStrategy =
      ResultRowPersistenceStrategyFactory.resultRowPersistenceStrategy(objectMapper, persistenceQueue, config)

   /**
    * Shutdown subscription to query history queue and clear down the queue files
    */
   fun shutDown() {
      logger.info { "Query result handler shutting down - $queryId" }
      queryHistoryDao.persistSankeyChart(queryId, sankeyViewBuilder)
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
            is StreamingQueryCancelledEvent -> processStreamingQueryCancelledEvent(event)
         }
      }
   }

   private fun persistEvent(event: QueryStartEvent) {
      logger.info { "Recording that the query ${event.queryId} has started. The query is:\n${event.taxiQuery}" }

      createQuerySummaryRecord(event.queryId) {
         QueryResultEventMapper.toQuerySummary(event)
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
      if (!config.persistRemoteCallMetadata && !config.persistRemoteCallResponses && !config.persistResults) {
         return
      }

      // Here, we're writing the operation invocations.
      // These can also be persisted during persistence of the result record.
      // However, Traversing all the OperationResult entries to get the
      // grandparent operation results from parameters is quite tricky.
      // Instead, we're capturing them out-of-band.
      val lineageRecords =
         resultRowPersistenceStrategy.createLineageRecords(listOf(operation.asOperationReferenceDataSource()), queryId)
      lineageRecords.forEach { persistenceQueue.storeLineageRecord(it) }
      appendOperationResultToSankeyChart(operation, this.sankeyViewBuilder)
      recordOperationMetadata(operation, queryId)
   }

   private fun recordOperationMetadata(operation: OperationResult, queryId: String) {
      persistenceQueue.storeRemoteCallResponse(
         RemoteCallResponse.fromRemoteCall(
            operation.remoteCall,
            queryId,
            objectMapper,
            config.persistRemoteCallResponses
         )
      )
   }

   fun finalize() {
      logger.debug { "PersistingQueryEventConsumer being finalized for query id $queryId now" }
   }

}
