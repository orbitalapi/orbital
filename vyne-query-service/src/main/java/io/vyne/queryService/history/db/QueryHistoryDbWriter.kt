package io.vyne.queryService.history.db

import arrow.core.extensions.list.functorFilter.filter
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Stopwatch
import com.google.common.cache.CacheBuilder
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.vyne.models.OperationResult
import io.vyne.models.TypedObject
import io.vyne.models.json.Jackson
import io.vyne.query.QueryResponse
import io.vyne.query.RemoteCallOperationResultHandler
import io.vyne.query.history.LineageRecord
import io.vyne.query.history.QueryResultRow
import io.vyne.query.history.QuerySummary
import io.vyne.query.history.RemoteCallResponse
import io.vyne.queryService.history.QueryCompletedEvent
import io.vyne.queryService.history.QueryEvent
import io.vyne.queryService.history.QueryEventConsumer
import io.vyne.queryService.history.QueryFailureEvent
import io.vyne.queryService.history.RestfulQueryExceptionEvent
import io.vyne.queryService.history.RestfulQueryResultEvent
import io.vyne.queryService.history.TaxiQlQueryExceptionEvent
import io.vyne.queryService.history.TaxiQlQueryResultEvent
import io.vyne.utils.timed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import lang.taxi.types.Type
import mu.KotlinLogging
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import reactor.core.scheduler.Schedulers
import reactor.util.function.Tuple2
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
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
   private val repository: QueryHistoryRecordRepository,
   private val persistenceQueue: HistoryPersistenceQueue,
   private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
   config: QueryHistoryConfig,
   private val scope: CoroutineScope

   ) : QueryEventConsumer, RemoteCallOperationResultHandler {
   private val createdQuerySummaryIds = CacheBuilder.newBuilder()
      .build<String, String>()
   val lastWriteTime = AtomicLong(System.currentTimeMillis())
   private val resultRowPersistenceStrategy = ResultRowPersistenceStrategyFactory.ResultRowPersistenceStrategy(objectMapper, persistenceQueue, config)


    /**
    * Shutdown subscription to query history queue and clear down the queue files
    */
   fun shutDown() {
      logger.info { "Query result handler shutting down - $queryId" }
   }

   override fun handleEvent(event: QueryEvent) {
      scope.launch {
         lastWriteTime.set(System.currentTimeMillis())
         when (event) {
            is TaxiQlQueryResultEvent -> persistEvent(event)
            is RestfulQueryResultEvent -> persistEvent(event)
            is QueryCompletedEvent -> persistEvent(event)
            is TaxiQlQueryExceptionEvent -> persistEvent(event)
            is QueryFailureEvent -> persistEvent(event)
            is RestfulQueryExceptionEvent -> persistEvent(event)
         }
      }
   }

   private fun persistEvent(event: QueryCompletedEvent) {

      logger.info { "Recording that query ${event.queryId} has completed" }

      createQuerySummaryRecord(event.queryId) {
         QuerySummary(
            queryId = event.queryId,
            clientQueryId = event.queryId,
            taxiQl = event.query,
            queryJson = objectMapper.writeValueAsString(event.query),
            endTime = event.timestamp,
            responseStatus = QueryResponse.ResponseStatus.ERROR,
            startTime = event.timestamp
         )
      }

      repository.setQueryEnded(
         event.queryId,
         event.timestamp,
         QueryResponse.ResponseStatus.COMPLETED,
         event.recordCount
      )

   }

   private fun persistEvent(event: RestfulQueryExceptionEvent) {
      createQuerySummaryRecord(event.queryId) {
         QuerySummary(
            queryId = event.queryId,
            clientQueryId = event.clientQueryId ?: event.queryId,
            taxiQl = null,
            queryJson = objectMapper.writeValueAsString(event.query),
            startTime = event.queryStartTime,
            responseStatus = QueryResponse.ResponseStatus.ERROR
         )
      }
      repository.setQueryEnded(event.queryId, event.timestamp, QueryResponse.ResponseStatus.ERROR, event.recordCount, event.message)

   }

   private fun persistEvent(event: TaxiQlQueryExceptionEvent) {
      createQuerySummaryRecord(event.queryId) {
         QuerySummary(
            queryId = event.queryId,
            clientQueryId = event.clientQueryId ?: event.queryId,
            taxiQl = event.query,
            queryJson = null,
            startTime = event.queryStartTime,
            responseStatus = QueryResponse.ResponseStatus.ERROR
         )
      }
      repository.setQueryEnded(event.queryId, event.timestamp, QueryResponse.ResponseStatus.ERROR, event.recordCount, event.message)

   }

   private fun persistEvent(event: QueryFailureEvent) {
      repository.setQueryEnded(
         event.queryId,
         Instant.now(),
         QueryResponse.ResponseStatus.ERROR,
         0,
         event.failure.message
      )
   }

   private fun persistEvent(event: RestfulQueryResultEvent) {
      createQuerySummaryRecord(event.queryId) {
         QuerySummary(
            queryId = event.queryId,
            clientQueryId = event.clientQueryId ?: event.queryId,
            taxiQl = null,
            queryJson = objectMapper.writeValueAsString(event.query),
            startTime = event.queryStartTime,
            responseStatus = QueryResponse.ResponseStatus.INCOMPLETE
         )
      }

      resultRowPersistenceStrategy.persistResultRowAndLineage(event)
   }

   private fun persistEvent(event: TaxiQlQueryResultEvent) {
      createQuerySummaryRecord(event.queryId) {
         try {
            val anonymousTypes = if (event.typedInstance.type.taxiType.anonymous && event.typedInstance is TypedObject) {
               val anonymousTypeForQuery =  event.anonymousTypes.firstOrNull { it.taxiType.qualifiedName ==  event.typedInstance.typeName}
               if (anonymousTypeForQuery == null) {
                  emptySet<Type>()
               } else {
                  setOf(anonymousTypeForQuery)
               }
            } else {
               emptySet<Type>()
            }
            QuerySummary(
               queryId = event.queryId,
               clientQueryId = event.clientQueryId ?: UUID.randomUUID().toString(),
               taxiQl = event.query,
               queryJson = null,
               startTime = event.queryStartTime,
               responseStatus = QueryResponse.ResponseStatus.INCOMPLETE,
               anonymousTypesJson = objectMapper.writeValueAsString(anonymousTypes)
            )
         } catch (e: Exception) {
            throw e
         }
      }
      resultRowPersistenceStrategy.persistResultRowAndLineage(event)
   }

   private fun createQuerySummaryRecord(queryId: String, factory: () -> QuerySummary) {
      // Since we don't have a "query started" concept (and it wouldn't
      // really work in a multi-threaded execution), we need to ensure that
      // the query object is present, as well as the result rows
      // Therefore, to avoid multiple trips to the db, we use a local
      // cache of created PersistentQuerySummary instances.
      // Note that this will fail when we allow execution across multiple JVM's.
      // At that point, we can simply wrap the insert in a try...catch, and let the
      // subsequent inserts fail.
      createdQuerySummaryIds.get(queryId) {
         val persistentQuerySummary = factory()
         try {
            repository.save(
               persistentQuerySummary
            )
            queryId
         } catch (e: Exception) {
            logger.warn(e) { "Constraint violation thrown whilst persisting query history record for query $queryId, will not try to persist again." }
            queryId
         }

      }
   }

   override fun recordResult(operation: OperationResult, queryId: String) {
      // Here, we're writing the operation invocations.
      // These can also be persisted during persistence of the result record.
      // However, Traversing all the OperationResult entries to get the
      // grandparent operation results from parameters is quite tricky.
      // Instead, we're captring them out-of-band.
      val lineageRecords = resultRowPersistenceStrategy.createLineageRecords(listOf(operation), queryId)
      lineageRecords.forEach { persistenceQueue.storeLineageRecord(it) }
   }

   fun finalize() {
      logger.debug { "PersistingQueryEventConsumer being finalized for query id $queryId now" }
   }

}

@Component
class QueryHistoryDbWriter(
   private val queryHistoryRecordRepository: QueryHistoryRecordRepository,
   private val resultRowRepository: QueryResultRowRepository,
   private val lineageRecordRepository: LineageRecordRepository,
   private val remoteCallResponseRepository: RemoteCallResponseRepository,
   private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
   // Visible for testing
   internal val config: QueryHistoryConfig = QueryHistoryConfig(),
   meterRegistry: MeterRegistry,
) {

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
         .subscribe(object : Subscriber<Tuple2<Long,RemoteCallResponse>> {
            override fun onSubscribe(subscription: Subscription) {
               remoteCallResponseSubscription = subscription
               logger.debug { "Subscribing to RemoteCallResponse Queue for All Queries" }
               subscription.request(1)
            }

            override fun onNext(rows: Tuple2<Long,RemoteCallResponse>) {
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
               if (rows.t1 % 500 == 0L) {logger.info { "Processing RemoteCallResponse on Queue for All Queries - position ${rows.t1}" } }
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
      } catch (e: JdbcSQLIntegrityConstraintViolationException) {
         logger.warn(e) { "Failed to persist lineage records, as a JdbcSQLIntegrityConstraintViolationException was thrown" }
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
   fun createEventConsumer(queryId: String): QueryEventConsumer {
      val persistingQueryEventConsumer = PersistingQueryEventConsumer(
         queryId,
         queryHistoryRecordRepository,
         persistenceQueue,
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

@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.history")
data class QueryHistoryConfig(
   /**
    * Defines the max payload size to persist.
    * Set to 0 to disable persisting the body of responses
    */
   val maxPayloadSizeInBytes: Int = 2048,
   // Mutable for testing
   var persistRemoteCallResponses: Boolean = true,
   // Page size for the historical Query Display in UI.
   val pageSize: Int = 20,

   // Mutable for testing
   var persistenceQueueStorePath: Path = Paths.get("./historyPersistenceQueue"),

   // Mutable for testing
   var persistResults: Boolean = true
)

