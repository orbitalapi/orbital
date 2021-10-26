package io.vyne.history.db

import arrow.core.extensions.list.functorFilter.filter
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Stopwatch
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.vyne.history.HistoryPersistenceQueue
import io.vyne.history.QueryHistoryConfig
import io.vyne.models.json.Jackson
import io.vyne.query.HistoryEventConsumerProvider
import io.vyne.query.QueryEventConsumer
import io.vyne.query.history.LineageRecord
import io.vyne.query.history.QueryResultRow
import io.vyne.query.history.RemoteCallResponse
import io.vyne.utils.timed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import mu.KotlinLogging
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.springframework.scheduling.annotation.Scheduled
import reactor.core.scheduler.Schedulers
import reactor.util.function.Tuple2
import java.sql.SQLIntegrityConstraintViolationException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

class QueryHistoryDbWriter(
   private val queryHistoryRecordRepository: QueryHistoryRecordRepository,
   private val resultRowRepository: QueryResultRowRepository,
   private val lineageRecordRepository: LineageRecordRepository,
   private val remoteCallResponseRepository: RemoteCallResponseRepository,
   private val sankeyChartRowRepository: QuerySankeyChartRowRepository,
   private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
   // Visible for testing
   internal val config: QueryHistoryConfig = QueryHistoryConfig(),
   meterRegistry: MeterRegistry
): HistoryEventConsumerProvider {

   var eventConsumers: ConcurrentHashMap<PersistingQueryEventConsumer, String> = ConcurrentHashMap()

   private val persistenceQueue = HistoryPersistenceQueue("combined", config.persistenceQueueStorePath)
   private val createdRemoteCallRecordIds = ConcurrentHashMap<String, String>()

   private var lineageSubscription: Subscription? = null
   private var resultRowSubscription: Subscription? = null
   private var remoteCallResponseSubscription: Subscription? = null
   private val historyDispatcher = Executors.newFixedThreadPool(10).asCoroutineDispatcher()

   init {

      persistenceQueue.retrieveNewResultRows().index()
         .publishOn(Schedulers.boundedElastic())
         .subscribe(object : Subscriber<Tuple2<Long, QueryResultRow>> {

            override fun onSubscribe(subscription: Subscription) {
               resultRowSubscription = subscription
               logger.debug { "Subscribing to QueryResultRow Queue for All Queries " }
               subscription.request(1)
            }

            override fun onNext(rows: Tuple2<Long, QueryResultRow>) {
               val duration = timed(TimeUnit.MILLISECONDS) {
                  resultRowRepository.save(rows.t2)
               }
               logger.trace { "Persistence of ${1} QueryResultRow records took ${duration}ms" }
               if (rows.t1 % 500 == 0L) { logger.info { "Processing QueryResultRows on Queue for All Queries - position ${rows.t1}" } }
               resultRowSubscription!!.request(1)
            }

            override fun onError(t: Throwable) {
               logger.error(t) { "Subscription to QueryResultRow Queue for All Queries encountered an error ${t.message}" }
            }

            override fun onComplete() {
               logger.info { "Subscription to QueryResultRow Queue for All Queries has completed" }
            }
         })

      persistenceQueue.retrieveNewLineageRecords().index()
         .publishOn(Schedulers.boundedElastic())
         .subscribe(object : Subscriber<Tuple2<Long, LineageRecord>> {
            override fun onSubscribe(subscription: Subscription) {
               lineageSubscription = subscription
               logger.debug { "Subscribing to LineageRecord Queue for All Queries" }
               subscription.request(1)
            }

            override fun onNext(lineageRecords: Tuple2<Long, LineageRecord>) {
               if (lineageRecords.t1 % 500 == 0L) { logger.info { "Processing LineageRecords on Queue for All Queries - position ${lineageRecords.t1}" } }
               persistLineageRecordBatch( listOf(lineageRecords.t2))
               lineageSubscription!!.request(1)
            }

            override fun onError(t: Throwable) {
               logger.error(t) { "Subscription to LineageRecord Queue for All Queries encountered an error" }
            }

            override fun onComplete() {
               logger.info { "Subscription to LineageRecord queue for All Queries  has completed" }
            }
         })

      persistenceQueue.retrieveNewRemoteCalls().index()
         .publishOn(Schedulers.boundedElastic())
         .subscribe(object : Subscriber<Tuple2<Long, RemoteCallResponse>> {
            override fun onSubscribe(subscription: Subscription) {
               remoteCallResponseSubscription = subscription
               logger.debug { "Subscribing to RemoteCallResponse Queue for All Queries" }
               subscription.request(1)
            }

            override fun onNext(rows: Tuple2<Long, RemoteCallResponse>) {
               rows.let { remoteCallResponse ->
                  createdRemoteCallRecordIds.computeIfAbsent(remoteCallResponse.t2.responseId) {
                     try {
                        remoteCallResponseRepository.save(remoteCallResponse.t2)
                     } catch (exception: Exception) {
                        logger.warn { "Attempting to re-save an already saved Remote Call ${exception.message}" }
                     }
                     remoteCallResponse.t2.responseId
                  }
               }
               if (rows.t1 % 500 == 0L) {
                  logger.info { "Processing RemoteCallResponse on Queue for All Queries - position ${rows.t1}" } }
               remoteCallResponseSubscription?.request(1)
            }

            override fun onError(t: Throwable?) {
               logger.error { "Subscription to RemoteCallResponse Queue for All Queries encountered an error ${t?.message}" }
            }

            override fun onComplete() {
               logger.info { "Subscription to RemoteCallResponse Queue for All Queries has completed" }
            }
         })


   }

   fun queryStart() {

   }

   private fun persistLineageRecordBatch(lineageRecords: List<LineageRecord>) {
      val sw = Stopwatch.createStarted()
      val existingRecords =
         lineageRecordRepository.findAllById(lineageRecords.map { it.dataSourceId })
            .map { it.dataSourceId }
      val newRecords = lineageRecords.filter { !existingRecords.contains(it.dataSourceId) }
      try {
         lineageRecordRepository.saveAll(newRecords)
      } catch (e: SQLIntegrityConstraintViolationException) {
         logger.warn(e) { "Failed to persist lineage records, as a SQLIntegrityConstraintViolationException was thrown" }
      }
      logger.debug {
         "Persistence batch of ${lineageRecords.size} LineageRecords (filtered to ${newRecords.size}) took ${
            sw.elapsed(
               TimeUnit.MILLISECONDS
            )
         }ms"
      }
   }

   var persistedQueryHistoryRecordsCounter: Counter = Counter
      .builder("queryHistoryRecords")
      .description("Number of records persisted to history")
      .register(meterRegistry)


   /**
    * Returns a new short-lived QueryEventConsumer.
    * This consumer should only be used for a single query, as it maintains some
    * state / caching in order to reduce the number of DB trips.
    */
   override fun createEventConsumer(queryId: String): QueryEventConsumer {
      val persistingQueryEventConsumer = PersistingQueryEventConsumer(
         queryId,
         queryHistoryRecordRepository,
         persistenceQueue,
         sankeyChartRowRepository,
         objectMapper,
         config,
         CoroutineScope(historyDispatcher)
      )
      eventConsumers[persistingQueryEventConsumer] = queryId
      return persistingQueryEventConsumer
   }

   /**
    * Every 30 seconds clear down any history writers that are complete - the eventConsumer contains db keys that should be
    * GC when the query has finished
    */
   @Scheduled(fixedDelay = 30000)
   fun cleanup() {
      val entriesTobeRemoved = mutableListOf<MutableMap.MutableEntry<PersistingQueryEventConsumer, String>>()
      val currentMillis = System.currentTimeMillis()
      eventConsumers.entries.forEach {
         if ((currentMillis - it.key.lastWriteTime.get()) > 120000) {
            logger.info { "Query ${it.value} is not expecting any more results .. shutting down result writer" }
            it.key.shutDown()
            entriesTobeRemoved.add(it)
         }
      }

      eventConsumers.entries.removeAll(entriesTobeRemoved)
   }

}
