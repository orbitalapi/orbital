package io.vyne.queryService.history.db

import arrow.core.extensions.list.functorFilter.filter
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.cache.CacheBuilder
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
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors


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
   private val repository: QueryHistoryRecordRepository,
   private val resultRowRepository: QueryResultRowRepository,
   private val lineageRecordRepository: LineageRecordRepository,
   private val remoteCallResponseRepository: RemoteCallResponseRepository,
   private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
   private val config: QueryHistoryConfig,
   private val dispatcher: ExecutorCoroutineDispatcher
) : QueryEventConsumer, RemoteCallOperationResultHandler {
   private val converter = TypedInstanceConverter(TypeNamedInstanceMapper)
   private val createdQuerySummaryIds = CacheBuilder.newBuilder()
      .build<String, String>()

   //TODO this is a ever increasing map - a leak
   private val createdLineageRecordIds = ConcurrentHashMap<String, String>()
   private val createdRemoteCallRecordIds = ConcurrentHashMap<String, String>()

   override fun handleEvent(event: QueryEvent): Job = GlobalScope.launch(dispatcher) {

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
      resultRowRepository.countAllByQueryId(event.queryId)
         .flatMap { recordCount ->
            repository.setQueryEnded(
               event.queryId,
               event.timestamp,
               QueryResponse.ResponseStatus.COMPLETED
            )
         }.subscribe()
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
         .subscribe()
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
         .subscribe()
   }

   private fun persistEvent(event: QueryFailureEvent) {
      repository.setQueryEnded(
         event.queryId,
         Instant.now(),
         QueryResponse.ResponseStatus.ERROR,
         event.failure.message
      )
         .subscribe()
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
      resultRowRepository.save(
         QueryResultRow(
            queryId = event.queryId,
            json = objectMapper.writeValueAsString(convertedTypedInstance),
            valueHash = event.typedInstance.hashCodeWithDataSource
         )
      ).block()

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
                  remoteCallResponseRepository.save(remoteCallRecord).block()
                  createdRemoteCallRecordIds.putIfAbsent(operationResult.id, operationResult.id)
               } catch (exception: Exception) {
                  //We expect failures here as multiple threads are writing to the same remoteCallId
                  logger.warn { "Unable to save remote call record ${exception.message}" }
               }
            }
      }

      val lineageRecords = createLineageRecords(dataSources.map { it.second }, event.queryId)
      saveLineageRecords(lineageRecords)
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
               val recordAlreadyPersisted = previousLineageRecordId != null;
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


   private fun saveLineageRecords(lineageRecords: List<LineageRecord>) {
      lineageRecords.forEach {
         try {
            lineageRecordRepository.save(it).block()
         } catch (exception: Exception) {
            logger.debug { "Unable to save lineage record ${exception.message}" }
         }
      }
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
            ).block()
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
      saveLineageRecords(lineageRecords)
   }
}

@Component
class QueryHistoryDbWriter(
   private val repository: QueryHistoryRecordRepository,
   private val resultRowRepository: QueryResultRowRepository,
   private val lineageRecordRepository: LineageRecordRepository,
   private val remoteCallResponseRepository: RemoteCallResponseRepository,
   private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
   private val config: QueryHistoryConfig = QueryHistoryConfig()
) {

   val persistenceDispatcher = Executors.newFixedThreadPool(2).asCoroutineDispatcher()

   /**
    * Returns a new short-lived QueryEventConsumer.
    * This consumer should only be used for a single query, as it maintains some
    * state / caching in order to reduce the number of DB trips.
    */
   fun createEventConsumer(): QueryEventConsumer {
      return PersistingQueryEventConsumer(
         repository,
         resultRowRepository,
         lineageRecordRepository,
         remoteCallResponseRepository,
         objectMapper,
         config,
         persistenceDispatcher
      )
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

