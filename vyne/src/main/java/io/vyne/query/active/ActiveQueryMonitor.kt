package io.vyne.query.active

import com.google.common.cache.CacheBuilder
import io.vyne.query.QueryResponse
import io.vyne.query.QueryResult
import io.vyne.utils.log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import lang.taxi.types.TaxiQLQueryString
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

val cancelledQueries = mutableSetOf<String>()

class ActiveQueryMonitor {
   val runningQueries:MutableMap<String,QueryResult?> = mutableMapOf()
   private val queryMetadataSink = MutableSharedFlow<RunningQueryStatus>()
   private val queryMetadataFlow = queryMetadataSink.asSharedFlow()
   private val runningQueryCache = CacheBuilder.newBuilder()
      .expireAfterWrite(Duration.ofMinutes(2)) // If we haven't heard from the query in 2 minutes, it's safe to assume it's dead
      .build<String, RunningQueryStatus>()


   //Map of clientQueryId to actual queryId - allows client to specify handle
   val queryIdToClientQueryIdMap = mutableMapOf<String, String>()

   /**
    * Return a SharedFlow of QueryMetaData events for given queryID creating event flows as necessary
    */
   fun queryStatusUpdates(queryId: String): Flow<RunningQueryStatus> {
      return queryMetadataFlow
         .filter { it.queryId == queryId }
   }

   fun cancelQuery(queryId: String) {
      cancelledQueries.add(queryId)
   }

   fun cancelQueryByClientQueryId(clientQueryId: String) {
      val queryId = queryIdToClientQueryIdMap.entries.filter { it.value == clientQueryId }.firstOrNull()?.key
      if (queryId != null) {
         cancelledQueries.add(queryId)
      }
   }

   fun allQueryStatusUpdates(): Flow<RunningQueryStatus> {
      return queryMetadataFlow
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
         RunningQueryStatus(queryId, startTime = Instant.now())
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
      log().debug("Reporting Query Starting - $queryId")

      runningQueries.putIfAbsent(queryId, null)

      storeAndEmit(queryId) {
         it.copy(state = QueryResponse.ResponseStatus.RUNNING, vyneQlQuery = query)

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
         log().info("Query $queryId estimated projection count now updated by $records to $updatedEstimate")
         queryStatus.copy(estimatedProjectionCount = updatedEstimate)
      }
   }

   fun reportComplete(queryId: String) {
      storeAndEmit(queryId) { runningQueryStatus ->
         runningQueryStatus
            .copy(
               running = false,
               state = QueryResponse.ResponseStatus.COMPLETED)
      }
      runningQueryCache.invalidate(queryId)
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

}

data class RunningQueryStatus(
   val queryId: String,
   val vyneQlQuery: TaxiQLQueryString? = null,
   val completedProjections: Int = 0,
   val estimatedProjectionCount: Int = 0,
   val startTime: Instant,
   val running: Boolean = true,
   val state: QueryResponse.ResponseStatus = QueryResponse.ResponseStatus.UNKNOWN
)

fun isQueryCancelled(queryId:String):Boolean {
   return cancelledQueries.contains(queryId)
}
