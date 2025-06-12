package com.orbitalhq.history.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Throwables
import com.google.common.cache.CacheBuilder
import com.orbitalhq.history.HistoryPersistenceQueue
import com.orbitalhq.history.QueryAnalyticsConfig
import com.orbitalhq.models.json.Jackson
import com.orbitalhq.query.HistoryEventConsumerProvider
import com.orbitalhq.query.QueryEventConsumer
import com.orbitalhq.query.history.LineageRecord
import com.orbitalhq.query.tracing.TracingEventSink
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.Schema
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import mu.KotlinLogging
import reactor.util.function.Tuple2
import java.time.Duration
import java.util.concurrent.Executors

private val logger = KotlinLogging.logger {}

class QueryHistoryDbWriter(
   queryHistoryRecordRepository: QueryHistoryRecordRepository,
   resultRowRepository: QueryResultRowRepository,
   lineageRecordRepository: LineageRecordRepository,
   remoteCallResponseRepository: RemoteCallResponseRepository,
   sankeyChartRowRepository: QuerySankeyChartRowRepository,
   errorEventRowRepository: QueryErrorEventRowRepository,
   private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
   // Visible for testing
   internal val config: QueryAnalyticsConfig = QueryAnalyticsConfig(),
   private val persistenceQueue: HistoryPersistenceQueue = HistoryPersistenceQueue(
      "combined",
      config.persistenceQueueStorePath
   )
) : HistoryEventConsumerProvider {

   private val queryHistoryDao = QueryHistoryDao(
      queryHistoryRecordRepository,
      resultRowRepository,
      lineageRecordRepository,
      remoteCallResponseRepository,
      sankeyChartRowRepository,
      errorEventRowRepository
   )

   private val createdRemoteCallRecordIds = CacheBuilder.newBuilder()
      .maximumSize(2000L)
      .expireAfterAccess(Duration.ofSeconds(60))
      .build<String, String>()

   private val historyDispatcher = Executors.newFixedThreadPool(10).asCoroutineDispatcher()

   init {

      persistenceQueue.retrieveNewResultRows().index()
         .publishOn(QuerySummaryPersister.queryHistoryScheduler)
         .bufferTimeout(config.writerMaxBatchSize, config.writerMaxDuration)
         .doOnError { t ->
            val rootCause = Throwables.getRootCause(t)
            logger.error(rootCause) { "Subscription to QueryResultRow Queue for All Queries encountered an error ${rootCause.message}" }
         }
         .doOnComplete {
            logger.debug { "Subscription to QueryResultRow Queue for All Queries has completed" }
         }
         .subscribe { batch ->
            val resultRows = batch.map { it.t2 }
            queryHistoryDao.saveQueryResultRows(resultRows)
            val latestIndex = batch.lastOrNull()?.t1 ?: -1
            logger.debug { "Processing QueryResultRows on Queue for All Queries - position $latestIndex" }
         }

      persistenceQueue.retrieveNewErrorEvents().index()
         .publishOn(QuerySummaryPersister.queryHistoryScheduler)
         .bufferTimeout(config.writerMaxBatchSize, config.writerMaxDuration)
         .doOnError { t ->
            val rootCause = Throwables.getRootCause(t)
            logger.error(rootCause) { "Subscription to QueryErrorEvent Queue for All Queries encountered an error ${rootCause.message}" }
         }
         .doOnComplete {
            logger.info { "Subscription to QueryErrorEvent Queue for All Queries has completed" }
         }
         .subscribe { batch ->
            val resultRows = batch.map { it.t2 }
            queryHistoryDao.saveQueryErrorRows(resultRows)
            val latestIndex = batch.lastOrNull()?.t1 ?: -1
            logger.debug { "Processing QueryErrorEvent on Queue for All Queries - position $latestIndex" }
         }
      persistenceQueue.retrieveNewLineageRecords().index()
         .publishOn(QuerySummaryPersister.queryHistoryScheduler)
         .bufferTimeout(config.writerMaxBatchSize, config.writerMaxDuration)
         .doOnError { error ->
            val rootCause = Throwables.getRootCause(error)
            logger.error(rootCause) { "Subscription to LineageRecord Queue for All Queries encountered an error - ${rootCause.message}" }
         }
         .doOnComplete {
            logger.debug { "Subscription to LineageRecord queue for All Queries  has completed" }
         }
         .subscribe { batch: MutableList<Tuple2<Long, LineageRecord>> ->
            try {
               val records = batch.map { it.t2 }
               queryHistoryDao.persistLineageRecordBatch(records)
               logger.info { "Processing LineageRecords on Queue for All Queries - position ${batch.lastOrNull()?.t1}" }
            } catch (e: Exception) {
               val rootCause = Throwables.getRootCause(e)
               logger.error(rootCause) { "Error processing LineageRecords on Queue for All Queries - ${rootCause.message}" }
            }
         }


      persistenceQueue.retrieveNewRemoteCalls().index()
         .publishOn(QuerySummaryPersister.queryHistoryScheduler)
         .bufferTimeout(config.writerMaxBatchSize, config.writerMaxDuration)
         .doOnError { error ->
            val rootCause = Throwables.getRootCause(error)
            logger.error(rootCause) { "Subscription to RemoteCallResponse Queue for All Queries encountered an error ${rootCause.message}" }
         }
         .doOnComplete { logger.info { "Subscription to RemoteCallResponse Queue for All Queries has completed" } }
         .subscribe { batch ->
            if (!config.persistRemoteCallResponses && !config.persistRemoteCallMetadata) {
               return@subscribe
            }
            val batchToWrite = batch
               .filter { record -> createdRemoteCallRecordIds.getIfPresent(record.t2.responseId) == null }

            try {
               queryHistoryDao.saveRemoteCallResponses(batchToWrite.map { it.t2 })
               logger.debug { "Processing RemoteCallResponse on Queue for All Queries - position ${batch.lastOrNull()?.t1}" }
            } catch (e: Exception) {
               val rootCause = Throwables.getRootCause(e)
               logger.warn(rootCause) { "Persisting batch of remote call responses failed: ${rootCause.message}" }
            }
         }
   }

   /**
    * Returns a new short-lived QueryEventConsumer.
    * This consumer should only be used for a single query, as it maintains some
    * state / caching in order to reduce the number of DB trips.
    */
   override fun createEventConsumer(queryId: String, schema: Schema): QueryEventConsumer {
      val persistingQueryEventConsumer = PersistingQueryEventConsumer(
         queryId,
         queryHistoryDao,
         persistenceQueue,
         objectMapper,
         config,
         CoroutineScope(historyDispatcher),
         schema
      )
      return persistingQueryEventConsumer
   }

   override fun createTraceEventSink(
      queryId: String,
      traceId: String,
      schema: Schema,
      queryOptions: QueryOptions
   ): TracingEventSink {
      val eventMetadataMapper = ContextAwareEventMetadataMapper(
         queryOptions, objectMapper, config, schema
      )
      return QueueWritingTraceEventSink(
         persistenceQueue, eventMetadataMapper
      )

   }
}
