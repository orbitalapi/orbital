package io.vyne.queryService.history.db

import arrow.core.extensions.list.functorFilter.filter
import ch.streamly.chronicle.flux.ChronicleStore
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.cache.CacheBuilder
import com.google.common.primitives.Ints
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.vyne.models.DataSource
import io.vyne.models.OperationResult
import io.vyne.models.StaticDataSource
import io.vyne.models.TypeNamedInstanceMapper
import io.vyne.models.TypedInstanceConverter
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
import io.vyne.queryService.history.QueryResultEvent
import io.vyne.queryService.history.RestfulQueryExceptionEvent
import io.vyne.queryService.history.RestfulQueryResultEvent
import io.vyne.queryService.history.TaxiQlQueryExceptionEvent
import io.vyne.queryService.history.TaxiQlQueryResultEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
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
   private val resultRowRepository: QueryResultRowRepository,
   private val lineageRecordRepository: LineageRecordRepository,
   private val remoteCallResponseRepository: RemoteCallResponseRepository,
   private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
   private val config: QueryHistoryConfig,
   private val scope: CoroutineScope,
   private val queryHistoryRecordsCounter: Counter,
   private val persistenceBufferSize: Int,
   private val persistenceBufferDuration: Duration
) : QueryEventConsumer, RemoteCallOperationResultHandler {
   private val converter = TypedInstanceConverter(TypeNamedInstanceMapper)

   private val createdQuerySummaryIds = CacheBuilder.newBuilder()
      .build<String, String>()

   private val createdLineageRecordIds = ConcurrentHashMap<String, String>()
   private val createdRemoteCallRecordIds = ConcurrentHashMap<String, String>()

   private val lineageRecordSink = Sinks.many().unicast().onBackpressureBuffer<LineageRecord>()
   private val remoteCallResponseSink = Sinks.many().unicast().onBackpressureBuffer<RemoteCallResponse>()

   private val rowSaveCounter = AtomicInteger(0)
   val lastWriteTime = AtomicLong(System.currentTimeMillis())


   val chronicleStore: ChronicleStore<QueryResultRow> =
      ChronicleStore("./history/$queryId",
         { queryResultRow -> queryResultRowToBinary(queryResultRow)},
         { bytes -> queryResultRowFromBinary(bytes)}
      )

   /**
    * Convert a QueryResultRow to ByteArray - extremely flaky and change to QueryResultRow
    * will break this
    */
   private fun queryResultRowToBinary(queryResultRow: QueryResultRow): ByteArray? {

      val queryId: ByteArray = queryResultRow.queryId.toByteArray(Charsets.UTF_8)
      val valueHash : ByteArray = Ints.toByteArray(queryResultRow.valueHash)
      val json: ByteArray = queryResultRow.json.toByteArray(Charsets.UTF_8)

      val result = ByteArray(queryId.size + json.size + valueHash.size)

      System.arraycopy(queryId, 0, result, 0, queryId.size)
      System.arraycopy(valueHash, 0, result, queryId.size, valueHash.size)
      System.arraycopy(json, 0, result, queryId.size + valueHash.size, json.size)

      return result
   }

   /**
    * Convert a ByteArray to QueryResultRow - extremely flaky and change to QueryResultRow
    * will break this
    */
   private fun queryResultRowFromBinary(bytes: ByteArray): QueryResultRow? {
      val queryId = ByteArray(36) //UUID length
      val valueHash = ByteArray(4) // Size of Int
      val json = ByteArray(bytes.size - 40)  //Rest is json

      System.arraycopy(bytes, 0, queryId, 0, queryId.size)
      System.arraycopy(bytes, 36, valueHash, 0, valueHash.size)
      System.arraycopy(bytes, 40, json, 0, json.size)

      return QueryResultRow(queryId = String(queryId, Charsets.UTF_8), valueHash = Ints.fromByteArray(valueHash), json = String(json, Charsets.UTF_8))
   }

   var resultRowSubscription: Subscription? = null

   init {

      chronicleStore.retrieveNewValues()
         .bufferTimeout(persistenceBufferSize, persistenceBufferDuration)
         .publishOn(Schedulers.boundedElastic())
         .subscribe(object : Subscriber<List<QueryResultRow>> {

            override fun onSubscribe(subscription: Subscription) {
               resultRowSubscription = subscription
               logger.info { "Subscribing to QueryResultRow Queue for Query $queryId" }
               subscription.request(1)
            }

            override fun onNext(rows: List<QueryResultRow>?) {
               resultRowRepository.saveAll(rows)
               val count = rowSaveCounter.addAndGet(rows!!.size)
               lastWriteTime.set(System.currentTimeMillis())

               logger.info { "Processing QueryResultRows on Queue for Query $queryId - count ${rows!!.size} position ${count}" }

               resultRowSubscription?.request(1)
            }

            override fun onError(t: Throwable?) {
               logger.info { "Subscription to QueryResultRow Queue for Query $queryId encountered an error ${t?.message}" }
            }

            override fun onComplete() {
               logger.info { "Subscription to QueryResultRow Queue for Query $queryId has completed" }
            }
      })

      lineageRecordSink.asFlux()
         .bufferTimeout(persistenceBufferSize, persistenceBufferDuration)
         .publishOn(Schedulers.boundedElastic())
         .subscribe { lineageRecords ->
            logger.info { "Persisting ${lineageRecords.size} Lineage records" }
            val existingRecords = lineageRecordRepository.findAllById(lineageRecords.map { it.id })
               .map { it.dataSourceId }

            val newRecords = lineageRecords.filter { !existingRecords.contains(it.dataSourceId) }
            try {
               lineageRecordRepository.saveAll(newRecords)
            } catch (e:  JdbcSQLIntegrityConstraintViolationException) {
               logger.warn { "Failed to persist lineage records, as a JdbcSQLIntegrityConstraintViolationException was thrown" }
            }

         }

      remoteCallResponseSink.asFlux()
         .bufferTimeout(persistenceBufferSize, persistenceBufferDuration)
         .publishOn(Schedulers.boundedElastic())
         .subscribe {
            logger.info { "Persisting ${it.size} Remote call responses " }
            remoteCallResponseRepository.saveAll(it)
         }

   }

   /**
    * Shutdown subscription to query history queue and clear down the queue files
    */
   fun shutDown() {
      logger.debug { "Shuttdown subsciption now" }
      resultRowSubscription?.cancel()

      try {
         File("./history/$queryId").listFiles().forEach {
            if (it.absolutePath.endsWith("cq4t") || it.absolutePath.endsWith("cq4")) {
               File(it.absolutePath).delete()
            }
         }
         File("./history/$queryId").delete()
      } catch (exception:Exception) {
         logger.warn { "Unable to delete history queue directory ${exception.message}" }
      }

   }

   override fun handleEvent(event: QueryEvent): Job = scope.launch {
      when (event) {
         is TaxiQlQueryResultEvent -> persistEvent(event)
         is RestfulQueryResultEvent -> persistEvent(event)
         is QueryCompletedEvent -> persistEvent(event)
         is TaxiQlQueryExceptionEvent -> persistEvent(event)
         is QueryFailureEvent -> persistEvent(event)
         is RestfulQueryExceptionEvent -> persistEvent(event)
      }
   }

   private fun persistEvent(event: QueryCompletedEvent) {
      logger.info { "Recording that query ${event.queryId} has completed" }
      repository.setQueryEnded(
         event.queryId,
         event.timestamp,
         QueryResponse.ResponseStatus.COMPLETED
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
      repository.setQueryEnded(event.queryId, event.timestamp, QueryResponse.ResponseStatus.ERROR, event.message)

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
      repository.setQueryEnded(event.queryId, event.timestamp, QueryResponse.ResponseStatus.ERROR, event.message)

   }

   private fun persistEvent(event: QueryFailureEvent) {
      repository.setQueryEnded(
         event.queryId,
         Instant.now(),
         QueryResponse.ResponseStatus.ERROR,
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
      persistResultRowAndLineage(event)
   }

   private fun persistEvent(event: TaxiQlQueryResultEvent) {
      createQuerySummaryRecord(event.queryId) {
         try {
            QuerySummary(
               queryId = event.queryId,
               clientQueryId = event.clientQueryId ?: UUID.randomUUID().toString(),
               taxiQl = event.query,
               queryJson = null,
               startTime = event.queryStartTime,
               responseStatus = QueryResponse.ResponseStatus.INCOMPLETE,
               anonymousTypesJson = objectMapper.writeValueAsString(event.anonymousTypes)
            )
         } catch (e: Exception) {
            throw e
         }
      }
      persistResultRowAndLineage(event)

   }

   private fun persistResultRowAndLineage(event: QueryResultEvent) {

      val (convertedTypedInstance, dataSources) = converter.convertAndCollectDataSources(event.typedInstance)
      chronicleStore.store(
         QueryResultRow(
            queryId = event.queryId,
            json = objectMapper.writeValueAsString(convertedTypedInstance),
            valueHash = event.typedInstance.hashCodeWithDataSource
         )
      )

      if (config.persistRemoteCallResponses) {
         dataSources
            .map { it.second }
            .filterIsInstance<OperationResult>()
            .distinctBy { it.remoteCall.responseId }
            .forEach { operationResult ->
               val responseJson = when (operationResult.remoteCall.response) {
                  null -> "No response body received"
                  // It's pretty rare to get a collection here, as the response value is the value before it's
                  // been deserialzied.  However, belts 'n' braces.
                  is Collection<*>, is Map<*, *> -> objectMapper.writeValueAsString(operationResult.remoteCall.response)
                  else -> operationResult.remoteCall.response.toString()
               }
               val remoteCallRecord = RemoteCallResponse(
                  responseId = operationResult.remoteCall.responseId,
                  remoteCallId = operationResult.remoteCall.remoteCallId,
                  queryId = event.queryId,
                  response = responseJson
               )
               try {
                  // Note that we check for the responseId, not the remoteCallId, as call-to-response is one-to-many
                  createdRemoteCallRecordIds.computeIfAbsent(operationResult.remoteCall.responseId) {
                     remoteCallResponseSink.tryEmitNext(remoteCallRecord)
                     operationResult.remoteCall.responseId
                  }
               } catch (exception: Exception) {
                  //We expect failures here as multiple threads are writing to the same remoteCallId
                  logger.warn { "Unable to save remote call record ${exception.message}" }
               }
            }
      }

      val lineageRecords = createLineageRecords(dataSources.map { it.second }, event.queryId)
      lineageRecords.forEach { lineageRecordSink.tryEmitNext(it) }

   }

   private fun createLineageRecords(
      dataSources: List<DataSource>,
      queryId: String
   ): List<LineageRecord> {
      val lineageRecords = dataSources
         .filter { it !is StaticDataSource }
         .distinctBy { it.id }
         .flatMap { discoveredDataSource ->
            // Store the id of the lineage record we're creating in a hashmap.
            // If we get a value back, that means that the record has already been created,
            // so we don't need to persist it, and return null from this mapper
            (listOf(discoveredDataSource) + discoveredDataSource.failedAttempts).mapNotNull { dataSource ->
               val previousLineageRecordId = createdLineageRecordIds.putIfAbsent(dataSource.id, dataSource.id)
               val recordAlreadyPersisted = previousLineageRecordId != null
               if (recordAlreadyPersisted) null else LineageRecord(
                  dataSource.id,
                  queryId,
                  dataSource.name,
                  objectMapper.writeValueAsString(dataSource)
               )
            }

         }
      return lineageRecords
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
            logger.debug { "Creating query history record for query $queryId" }
            repository.save(
               persistentQuerySummary
            )
            logger.info { "Query history record for query $queryId created successfully" }
            queryId
         } catch (e: Exception) {
            logger.debug { "Constraint violation thrown whilst persisting query history record for query $queryId, will not try to persist again." }
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
      val lineageRecords = createLineageRecords(listOf(operation), queryId)
      lineageRecords.forEach { lineageRecordSink.tryEmitNext(it) }
   }

   fun finalize() {
      println("PersistingQueryEventConsumer being finalized now")
   }

}

@Component
class QueryHistoryDbWriter(
   private val repository: QueryHistoryRecordRepository,
   private val resultRowRepository: QueryResultRowRepository,
   private val lineageRecordRepository: LineageRecordRepository,
   private val remoteCallResponseRepository: RemoteCallResponseRepository,
   private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
   private val config: QueryHistoryConfig = QueryHistoryConfig(),
   private val meterRegistry: MeterRegistry,

   // Have made these mutable to allow for easier testing
   var persistenceBufferSize: Int = 250,
   var persistenceBufferDuration: Duration = Duration.ofMillis(1000)
) {

   var eventConsumers: WeakHashMap<PersistingQueryEventConsumer, String> = WeakHashMap()

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
         repository,
         resultRowRepository,
         lineageRecordRepository,
         remoteCallResponseRepository,
         objectMapper,
         config,
         CoroutineScope(Executors.newFixedThreadPool(1).asCoroutineDispatcher()),
         persistedQueryHistoryRecordsCounter,
         persistenceBufferSize,
         persistenceBufferDuration
      )
      eventConsumers[persistingQueryEventConsumer] = queryId
      return persistingQueryEventConsumer
   }

   /**
    * Every 30 seconds clear down any history writers that are complete
    */
   @Scheduled(fixedDelay = 30000)
   fun cleanup() {
      eventConsumers.entries.forEach {
         if ( (System.currentTimeMillis() - it.key.lastWriteTime.get()) > 60000 ) {
            logger.debug { "Query ${it.value} is not expecting any more results .. shutting it result writer" }
            it.key.shutDown()
         }
      }
      eventConsumers.entries.removeIf { (System.currentTimeMillis() - it.key.lastWriteTime.get()) > 60000 }
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
   val persistRemoteCallResponses: Boolean = true,
   // Page size for the historical Query Display in UI.
   val pageSize: Int = 20
)

