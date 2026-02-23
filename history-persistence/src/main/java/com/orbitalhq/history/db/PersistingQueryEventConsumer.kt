package com.orbitalhq.history.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.history.HistoryPersistenceQueue
import com.orbitalhq.history.QueryAnalyticsConfig
import com.orbitalhq.history.QueryResultEventMapper
import com.orbitalhq.history.ResultRowPersistenceStrategyFactory
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.json.Jackson
import com.orbitalhq.query.QueryCompletedEvent
import com.orbitalhq.query.QueryErrorStreamEvent
import com.orbitalhq.query.QueryEvent
import com.orbitalhq.query.QueryEventConsumer
import com.orbitalhq.query.QueryFailureEvent
import com.orbitalhq.query.Query
import com.orbitalhq.query.QueryStartEvent
import com.orbitalhq.query.RemoteCallOperationResultHandler
import com.orbitalhq.query.RestfulQueryExceptionEvent
import com.orbitalhq.query.RestfulQueryResultEvent
import com.orbitalhq.query.StreamingQueryCancelledEvent
import com.orbitalhq.query.TaxiQlQueryExceptionEvent
import com.orbitalhq.query.TaxiQlQueryResultEvent
import com.orbitalhq.query.history.RemoteCallResponse
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.query.TaxiQLQueryString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.time.Instant
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

   /**
    * Lazily executed coroutine jobs are passed to the channel to be run in sequence.
    */
   private val channel = Channel<Job>(capacity = Channel.UNLIMITED).apply {
      scope.launch {
         consumeEach { it.join() }
      }
   }
   val lastWriteTime = AtomicLong(System.currentTimeMillis())

   private val resultRowPersistenceStrategy =
      ResultRowPersistenceStrategyFactory.resultRowPersistenceStrategy(objectMapper, persistenceQueue, config)

   /**
    * Shutdown subscription to query history queue and clear down the queue files
    */
   override fun shutdown() {
      logger.debug { "Query result handler shutting down - $queryId" }
      super.terminateSubscription()
   }

   override fun captureQueryStart(
      queryId: String,
      timestamp: Instant,
      taxiQuery: TaxiQLQueryString?,
      query: Query?,
      clientQueryId: String,
      message: String,
      anonymousTypes: Set<Type>
   ) {
      handleEvent(
         QueryStartEvent(
            queryId = queryId,
            timestamp = timestamp,
            taxiQuery = taxiQuery,
            query = query,
            clientQueryId = clientQueryId,
            message = message,
            anonymousTypes = anonymousTypes,
            persistResults = config.persistResults,
            persistRemoteCallResponses = config.persistRemoteCallResponses,
            persistRemoteCallMetadata = config.persistRemoteCallMetadata,
            persistTraceEvents = config.persistTraceEvents,
            persistErrors = config.persistErrors
         )
      )
   }

   override fun handleEvent(event: QueryEvent) {
      logger.trace { "Launching coroutine to dispatch ${event.javaClass.name}" }
      /**
       * Our scope is backed by a thread pool. and a typical query yields the following event sequence:
       * QueryStartEvent
       * TaxiQlQueryResultEvent
       * QueryCompletedEvent
       *
       * If we were not to use the below approach, and directly call scope.launch { when(even) {....} }
       * QueryCompletedEvent can be processed before QueryStartEvent due to context switching, i.e.
       *
       * QueryStartEvent arrived here at T0
       * TaxiQlQueryResultEvent arrived here at T0 + 100 microseconds
       * QueryCompletedEvent arrived here T0 + 102 microseconds
       *
       * a simple scope.launch { } will process QueryStartEvent, TaxiQlQueryResultEvent and  QueryCompletedEvent
       * on 3 different threads as th1, th2 and th3. Due to context switching th3 can finish before th1 in which case
       * we failed to update the QUERY_SUMMARY table correctly and hence query history will present missing queries.
       *
       * To avoid his, we launch our jobs lazily here scope.launch(start = CoroutineStart.LAZY) and push the lazy jobs to a channel
       * which guarantees the sequential execution.
       */
      val job = scope.launch(start = CoroutineStart.LAZY) {
         logger.trace { "Launched coroutine to dispatch ${event.javaClass.name}" }
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
            is QueryErrorStreamEvent -> persistEvent(event)
         }

         if (event.isTerminalEvent) {
            handleTerminalEvent(event)
         }
      }
      if (logger.isTraceEnabled) {
         job.invokeOnCompletion { logger.trace { "coroutine for ${event.javaClass.name} is completed, ${scope.isActive}" } }
      }
      channel.trySend(job)
   }

   private fun handleTerminalEvent(event: QueryEvent) {
      this.shutdown()
   }

   private fun persistEvent(event: QueryStartEvent) {
      logger.debug { "Recording that the query ${event.queryId} has started. The query is:\n${event.taxiQuery}" }

      createQuerySummaryRecord(event.queryId) {
         QueryResultEventMapper.toQuerySummary(event)
      }
   }

   private fun persistEvent(event: QueryErrorStreamEvent) {
      resultRowPersistenceStrategy.persistErrorEvent(event)
   }

   private fun persistEvent(event: RestfulQueryResultEvent) {
      resultRowPersistenceStrategy.persistResultRowAndLineage(event)
      appendToSankeyChart(event.typedInstance, this.sankeyViewBuilder)
   }

   private fun persistEvent(event: TaxiQlQueryResultEvent) {
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

}
