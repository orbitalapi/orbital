package io.vyne.query.active

import com.google.common.cache.CacheBuilder
import io.vyne.query.EstimatedRecordCountUpdateHandler
import io.vyne.query.QueryContextEventBroker
import io.vyne.query.QueryContextEventHandler
import io.vyne.query.QueryResponse
import io.vyne.schemas.RemoteOperation
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import lang.taxi.query.TaxiQLQueryString
import mu.KotlinLogging
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

class ActiveQueryMonitor {
   private val queryBrokers = ConcurrentHashMap<String, QueryContextEventBroker>()
   private val queryMetadataSink = MutableSharedFlow<RunningQueryStatus>()
   private val queryMetadataFlow = queryMetadataSink.asSharedFlow()
   private val runningQueryCache = CacheBuilder.newBuilder()
      .expireAfterWrite(Duration.ofMinutes(2)) // If we haven't heard from the query in 2 minutes, it's safe to assume it's dead
      .build<String, RunningQueryStatus>()

   //Map of clientQueryId to actual queryId - allows client to specify handle
   val queryIdToClientQueryIdMap = ConcurrentHashMap<String, String>()

   /**
    * Return a SharedFlow of QueryMetaData events for given queryID creating event flows as necessary
    */
   fun queryStatusUpdates(queryId: String): Flow<RunningQueryStatus> {
      return queryMetadataFlow
         .filter { it.queryId == queryId }
   }

   fun cancelQuery(queryId: String):Boolean {
      val broker = queryBrokers[queryId]
      return if (broker != null) {
         broker.requestCancel()
         logger.info { "Requested cancellation of query $queryId" }
         true
      } else {
         logger.warn { "Cannot request cancellation of query $queryId as it was not found" }
         false
      }
   }

   fun cancelQueryByClientQueryId(clientQueryId: String): Boolean {
      val queryId = queryIdToClientQueryIdMap.entries.firstOrNull { it.value == clientQueryId }?.key
      return if (queryId != null) {
         cancelQuery(queryId)
         true
      } else {
         false
      }
   }

   fun allQueryStatusUpdates(): Flow<RunningQueryStatus> {
      return queryMetadataFlow
   }

   fun runningQueries(): Map<String, RunningQueryStatus> {
      return runningQueryCache.asMap()
   }

   /**
    * Return latest QueryMetaData from latest state change or UNKNOWN QueryMetaData
    */
   fun queryMetaData(queryId: String): RunningQueryStatus? {
      return runningQueryCache.getIfPresent(queryId)
   }

   private fun storeAndEmit(queryId: String, updater: (RunningQueryStatus) -> RunningQueryStatus) {
      var updaterCalled = false
      // Unfortunately, you can't use compute() on the underlying map of Guava if the value isn't
      // guaranteed to be present, as the docs state that changes made by compute do not update the keys of the cache.
      // So, we have to do a bit of hoop jumping here.
      val newDefaultStatus = {
         RunningQueryStatus(queryId, startTime = Instant.now(), queryType = QueryType.DETERMINANT) //
      }
      var updatedValue: RunningQueryStatus? = runningQueryCache.get(queryId) {
         val updatedValue = updater(newDefaultStatus())
         updaterCalled = true

         updatedValue
      }
      // If the value was already present, call compute.
      if (!updaterCalled) {
         updatedValue = runningQueryCache.asMap().compute(queryId) { _, value -> updater(value ?: newDefaultStatus()) }
      }
      if (updatedValue != null) {
         GlobalScope.launch { sendUpdate(updatedValue) }
      }
   }

   fun reportStart(queryId: String, clientQueryId: String?, query: TaxiQLQueryString) {
      logger.debug { "Reporting Query Starting - $queryId - [$query]" }

      val queryType = if (query.startsWith("stream")) QueryType.STREAMING else QueryType.DETERMINANT

      storeAndEmit(queryId) {
         it.copy(state = QueryResponse.ResponseStatus.RUNNING, vyneQlQuery = query, queryType = queryType)

      }
      if (clientQueryId != null) {
         queryIdToClientQueryIdMap.putIfAbsent(
            queryId,
            clientQueryId
         )
      }
   }

   fun incrementExpectedRecordCount(queryId: String, records: Int) {
      storeAndEmit(queryId) { queryStatus ->
         val updatedEstimate = queryStatus.estimatedProjectionCount + records
         logger.info{"Query $queryId estimated projection count now updated by $records to $updatedEstimate"}
         queryStatus.copy(estimatedProjectionCount = updatedEstimate)
      }
   }

   fun reportComplete(queryId: String) {
      storeAndEmit(queryId) { runningQueryStatus ->
         runningQueryStatus
            .copy(
               running = false,
               state = QueryResponse.ResponseStatus.COMPLETED
            )
      }
      queryBrokers.remove(queryId)
      //TODO Should we invalidate the cache or just allow expiry
      //runningQueryCache.invalidate(queryId)
   }


   fun incrementEmittedRecordCount(queryId: String) {
      storeAndEmit(queryId) { runningQueryStatus ->
         // It's safe to increment this way, as we're inside a blocking operation
         runningQueryStatus.copy(completedProjections = runningQueryStatus.completedProjections + 1)
      }
   }


   /**
    * Emit the metadata event for the given queryId
    */
   private suspend fun sendUpdate(metaData: RunningQueryStatus) {
      queryMetadataSink.emit(metaData)
   }

   fun eventDispatcherForQuery(queryId: String, handlers:List<QueryContextEventHandler> = emptyList()): QueryContextEventBroker {
      val broker = QueryContextEventBroker()
         .addHandlers(handlers)
         .addHandler(object : EstimatedRecordCountUpdateHandler {
         override fun reportIncrementalEstimatedRecordCount(operation: RemoteOperation, estimatedRecordCount: Int) {
            incrementExpectedRecordCount(queryId, estimatedRecordCount)
         }
      })
      this.queryBrokers[queryId] = broker
      return broker
   }

}

data class RunningQueryStatus(
   val queryId: String,
   val vyneQlQuery: TaxiQLQueryString? = null,
   val completedProjections: Int = 0,
   val estimatedProjectionCount: Int = 0,
   val startTime: Instant,
   val running: Boolean = true,
   val state: QueryResponse.ResponseStatus = QueryResponse.ResponseStatus.UNKNOWN,
   val queryType: QueryType
)

enum class QueryType {
   STREAMING,
   DETERMINANT
}
