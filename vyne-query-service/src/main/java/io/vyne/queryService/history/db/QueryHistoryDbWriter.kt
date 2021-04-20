package io.vyne.queryService.history.db

import arrow.core.extensions.list.functorFilter.filter
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.cache.CacheBuilder
import io.vyne.models.OperationResult
import io.vyne.models.StaticDataSource
import io.vyne.models.TypeNamedInstanceMapper
import io.vyne.models.TypedInstanceConverter
import io.vyne.models.json.Jackson
import io.vyne.query.QueryResponse
import io.vyne.query.history.LineageRecord
import io.vyne.query.history.QueryResultRow
import io.vyne.query.history.QuerySummary
import io.vyne.queryService.history.*
import io.vyne.utils.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

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
   private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
   private val config: QueryHistoryConfig
) : QueryEventConsumer {
   private val converter = TypedInstanceConverter(TypeNamedInstanceMapper)
   private val createdQuerySummaryIds = CacheBuilder.newBuilder()
      .build<String, String>()
   private val createdLineageRecordIds = ConcurrentHashMap<String, String>()

   override fun handleEvent(event: QueryEvent): Job = GlobalScope.launch(Dispatchers.IO) {

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
      log().info("Recording that query ${event.queryId} has completed")
      resultRowRepository.countAllByQueryId(event.queryId)
         .flatMap { recordCount ->
            repository.setQueryEnded(
               event.queryId,
               event.timestamp,
               recordCount,
               QueryResponse.ResponseStatus.COMPLETED
            )
         }.subscribe()
   }

   private fun persistEvent(event: RestfulQueryExceptionEvent) {
      createQuerySummaryRecord(event.queryId) {
         QuerySummary(
            queryId = event.queryId,
            clientQueryId = event.clientQueryId ?: UUID.randomUUID().toString(),
            taxiQl = null,
            queryJson = objectMapper.writeValueAsString(event.query),
            startTime = Instant.now(),
            responseStatus = QueryResponse.ResponseStatus.ERROR
         )
      }
      repository.setQueryEnded(event.queryId, event.timestamp, 0, QueryResponse.ResponseStatus.ERROR, event.message)
         .subscribe()
   }

   private fun persistEvent(event: TaxiQlQueryExceptionEvent) {
      createQuerySummaryRecord(event.queryId) {
         QuerySummary(
            queryId = event.queryId,
            clientQueryId = event.clientQueryId ?: UUID.randomUUID().toString(),
            taxiQl = event.query,
            queryJson = null,
            startTime = Instant.now(),
            responseStatus = QueryResponse.ResponseStatus.ERROR
         )
      }
      repository.setQueryEnded(event.queryId, event.timestamp, 0, QueryResponse.ResponseStatus.ERROR, event.message)
         .subscribe()
   }
   private fun persistEvent(event: QueryFailureEvent)  {
      repository.setQueryEnded(event.queryId, Instant.now(), 0, QueryResponse.ResponseStatus.ERROR, event.failure.message)
         .subscribe()
   }

   private fun persistEvent(event: RestfulQueryResultEvent)  {
      createQuerySummaryRecord(event.queryId) {
         QuerySummary(
            queryId = event.queryId,
            clientQueryId = event.clientQueryId ?: UUID.randomUUID().toString(),
            taxiQl = null,
            queryJson = objectMapper.writeValueAsString(event.query),
            startTime = Instant.now(),
            responseStatus = QueryResponse.ResponseStatus.INCOMPLETE
         )
      }

      resultRowRepository.save(
         QueryResultRow(
            queryId = event.queryId,
            json = objectMapper.writeValueAsString(converter.convert(event.typedInstance)),
            valueHash = event.typedInstance.hashCodeWithDataSource
         )
      ).subscribe()
   }

   private fun persistEvent(event: TaxiQlQueryResultEvent) {
      createQuerySummaryRecord(event.queryId) {
         try {
            QuerySummary(
               queryId = event.queryId,
               clientQueryId = event.clientQueryId ?: UUID.randomUUID().toString(),
               taxiQl = event.query,
               queryJson = null,
               startTime = Instant.now(),
               responseStatus = QueryResponse.ResponseStatus.INCOMPLETE,
               anonymousTypesJson = objectMapper.writeValueAsString(event.anonymousTypes)
            )
         } catch (e: Exception) {
            throw e
         }
      }
      val (convertedTypedInstance, dataSources) = converter.convertAndCollectDataSources(event.typedInstance)
      resultRowRepository.save(
         QueryResultRow(
            queryId = event.queryId,
            json = objectMapper.writeValueAsString(convertedTypedInstance),
            valueHash = event.typedInstance.hashCodeWithDataSource
         )
      ).block()
      val lineageRecords = dataSources.map { it.second }
         .filter { it !is StaticDataSource }
         .distinctBy { it.id }
         .map { dataSource ->
            when (dataSource) {
               is OperationResult -> trimResponseBodyWhenExceedsConfiguredMax(dataSource)
               else -> dataSource
            }
         }
         .mapNotNull { dataSource ->


            // Store the id of the lineage record we're creating in a hashmap.
            // If we get a value back, that means that the record has already been created,
            // so we don't need to persist it, and return null from this mapper
            val previousLineageRecordId = createdLineageRecordIds.putIfAbsent(dataSource.id, dataSource.id)
            val recordAlreadyPersisted = previousLineageRecordId != null;
            if (recordAlreadyPersisted) null else LineageRecord(
               dataSource.id,
               event.queryId,
               dataSource.name,
               objectMapper.writeValueAsString(dataSource)
            )
         }
      lineageRecordRepository.saveAll(lineageRecords)
         .collectList().block()

   }

   private fun trimResponseBodyWhenExceedsConfiguredMax(dataSource: OperationResult): OperationResult {
      val response = dataSource.remoteCall.response
      val sanitizedDataSource = if (response != null) {
         val responseSize = response.toString().toByteArray().size
         if (responseSize > config.maxPayloadSizeInBytes) {
            dataSource.copy(
               remoteCall = dataSource.remoteCall.copy(response = "Response has not been captured, as it's size $responseSize bytes exceeded max configured bytes (${config.maxPayloadSizeInBytes}).")
            )
         } else {
            dataSource
         }
      } else {
         dataSource
      }
      return sanitizedDataSource
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
            log().info("Creating query history record for query $queryId")
            val fromDb = repository.save(
               persistentQuerySummary
            ).block()
            log().info("Query history record for query $queryId created successfully")
            queryId
         } catch (e: Exception) {
            log().info("Constraint violation thrown whilst persisting query history record for query $queryId, will not try to persist again.")
            queryId
         }

      }
   }
}

@Component
class QueryHistoryDbWriter(
   private val repository: QueryHistoryRecordRepository,
   private val resultRowRepository: QueryResultRowRepository,
   private val lineageRecordRepository: LineageRecordRepository,
   private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
   private val config: QueryHistoryConfig = QueryHistoryConfig()
) {


   /**
    * Returns a new short-lived QueryEventConsumer.
    * This consumer should only be used for a single query, as it maintains some
    * state / caching in order to reduce the number of DB trips.
    */
   fun createEventConsumer(): QueryEventConsumer {
      return PersistingQueryEventConsumer(
         repository, resultRowRepository, lineageRecordRepository, objectMapper, config
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
   val maxPayloadSizeInBytes: Int = 2048
)

