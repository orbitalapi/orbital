package io.vyne.history.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.cache.CacheBuilder
import io.vyne.history.HistoryPersistenceQueue
import io.vyne.history.QueryHistoryConfig
import io.vyne.models.OperationResult
import io.vyne.models.TypedObject
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import lang.taxi.types.Type
import mu.KotlinLogging
import java.time.Instant
import java.util.UUID
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
   private val sankeyChartRowRepository: QuerySankeyChartRowRepository,
   private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
   config: QueryHistoryConfig,
   private val scope: CoroutineScope

) : QueryEventConsumer, RemoteCallOperationResultHandler {
   private val createdQuerySummaryIds = CacheBuilder.newBuilder()
      .build<String, String>()
   val lastWriteTime = AtomicLong(System.currentTimeMillis())
   private val resultRowPersistenceStrategy = ResultRowPersistenceStrategyFactory.resultRowPersistenceStrategy(objectMapper, persistenceQueue, config)
   private val sankeyViewBuilder = LineageSankeyViewBuilder()

   private var sankeyChartPersisted = false
   /**
    * Shutdown subscription to query history queue and clear down the queue files
    */
   fun shutDown() {
      logger.info { "Query result handler shutting down - $queryId" }
      if (!sankeyChartPersisted) {
         persistSankeyChart()
      }
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
            is QueryStartEvent -> persistEvent(event)
            else -> {} // noop
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

   private fun persistEvent(event: QueryCompletedEvent) {

      logger.info { "Recording that query ${event.queryId} has completed" }

      createQuerySummaryRecord(event.queryId) {
         QuerySummary(
            queryId = event.queryId,
            clientQueryId = event.clientQueryId ?: event.queryId,
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

      persistSankeyChart()
   }

   private fun persistSankeyChart() {
      val chartRows = sankeyViewBuilder.asChartRows(queryId)
      sankeyChartRowRepository.saveAll(chartRows)
      sankeyChartPersisted = true
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

      sankeyViewBuilder.append(event.typedInstance)
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
      sankeyViewBuilder.append(event.typedInstance)
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

      sankeyViewBuilder.captureOperationResult(operation)
   }

   fun finalize() {
      logger.debug { "PersistingQueryEventConsumer being finalized for query id $queryId now" }
   }

}
