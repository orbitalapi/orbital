package com.orbitalhq.query.runtime.core.monitor

import com.hazelcast.core.EntryEvent
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.map.IMap
import com.hazelcast.map.listener.EntryAddedListener
import com.hazelcast.map.listener.EntryUpdatedListener
import com.orbitalhq.logging.MDCContextKeys
import com.orbitalhq.query.EstimatedRecordCountUpdateHandler
import com.orbitalhq.query.QueryContextEventBroker
import com.orbitalhq.query.QueryContextEventHandler
import com.orbitalhq.query.QueryResponse
import com.orbitalhq.schemas.RemoteOperation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.runBlocking
import lang.taxi.query.QueryMode
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery
import mu.KotlinLogging
import org.slf4j.MDC
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

class ActiveQueryMonitor(private val hazelcast: HazelcastInstance):EntryUpdatedListener<String,RunningQueryStatus>, EntryAddedListener<String,RunningQueryStatus> {
   private val queryBrokers = ConcurrentHashMap<String, QueryContextEventBroker>()
   private val queryMetadataSink = MutableSharedFlow<RunningQueryStatus>()
   private val queryMetadataFlow = queryMetadataSink.asSharedFlow()
   private val runningQueryCache:IMap<String,RunningQueryStatus> = hazelcast.getMap("queryStatus")
   private val cancellationEventTopic = hazelcast.getTopic<QueryCancellationMessage>("cancellationEvents")

   //Map of clientQueryId to actual queryId - allows client to specify handle
   private val queryIdToClientQueryIdMap:IMap<String,String> = hazelcast.getMap("queryIdToClientQueryId")
   private val clientQueryIdToQueryIdMap:IMap<String,String> = hazelcast.getMap("clientQueryIdToQueryId")

   init {
       cancellationEventTopic.addMessageListener { message ->
          queryBrokers.computeIfPresent(message.messageObject.queryId) { id, broker ->
              // This is a HZ callback, so re-populate MDC with query Id.
              MDC.put(MDCContextKeys.QueryId, id)
              MDC.put(MDCContextKeys.ClientQueryId, message.messageObject.queryClientId)
             broker.requestCancel()
             logger.info { "Requested cancellation of query $id with query broker" }
             broker
          }
       }
      runningQueryCache.addEntryListener(this, true)
   }

   /**
    * Return a SharedFlow of QueryMetaData events for given queryID creating event flows as necessary
    */
   fun queryStatusUpdates(queryId: String): Flow<RunningQueryStatus> {
      return queryMetadataFlow
         .filter { it.queryId == queryId }
   }

  fun cancelQuery(queryId: String):Boolean {
      val clientQueryId = queryIdExists(queryId)
     return if (clientQueryId != null) {
         MDC.put(MDCContextKeys.QueryId, queryId)
         logger.info { "Dispatching cancellation request of query $queryId to topic" }
        cancellationEventTopic.publish(QueryCancellationMessage(queryId = queryId, queryClientId = clientQueryId))
        true
     } else {
        false
     }
   }

    fun queryIdFromClientId(clientQueryId: String): String? {
        return clientQueryIdToQueryIdMap[clientQueryId]
    }
   /**
    * Indicates if a query exists with the queryId anywhere in the cluster (not specifically on this node)
    */
   private fun queryIdExists(queryId:String): String? {
      return queryIdToClientQueryIdMap[queryId]
   }
   private fun clientQueryIdExists(clientQueryId:String): Boolean {
      return clientQueryIdToQueryIdMap.containsKey(clientQueryId)
   }

   fun cancelQueryByClientQueryId(clientQueryId: String): Boolean {
      return if (clientQueryIdExists(clientQueryId)) {
          MDC.put(MDCContextKeys.ClientQueryId, clientQueryId)
         val queryId = clientQueryIdToQueryIdMap[clientQueryId]!!
          MDC.put(MDCContextKeys.QueryId, queryId)
         cancelQuery(queryId)
      } else {
         false
      }
   }

   fun allQueryStatusUpdates(): Flow<RunningQueryStatus> {
      return queryMetadataFlow
   }

   fun runningQueries(): Map<String, RunningQueryStatus> {
      return runningQueryCache
   }

   /**
    * Return latest QueryMetaData from latest state change or UNKNOWN QueryMetaData
    */
   fun queryMetaData(queryId: String): RunningQueryStatus? {
      return runningQueryCache[queryId]
   }

   private fun storeAndEmit(queryId: String, updater: (RunningQueryStatus) -> RunningQueryStatus) {
      runningQueryCache.compute(queryId) { _, currentStatus ->
         val updatedValue = updater(currentStatus ?: RunningQueryStatus(queryId, startTime = Instant.now(), queryMode = QueryMode.FIND_ALL) )
         updatedValue
      }
      // Note: emitting the update is now done when we receive a change notification from the IMap.
      // This keeps update logic consistent across the cache
//      if (updatedValue != null) {
//         GlobalScope.launch { sendUpdate(updatedValue) }
//      }
   }

   fun reportStart(queryId: String, clientQueryId: String?, query: TaxiQlQuery) {
      logger.debug { "Reporting Query Starting - $queryId - [$query]" }

      storeAndEmit(queryId) {
         it.copy(state = QueryResponse.ResponseStatus.RUNNING, taxiQlQuery = query.source, queryMode = query.queryMode)

      }
      if (clientQueryId != null) {
         queryIdToClientQueryIdMap.putIfAbsent(
            queryId,
            clientQueryId
         )
         clientQueryIdToQueryIdMap.putIfAbsent(
            clientQueryId,queryId
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
      val clientQueryId = queryIdToClientQueryIdMap.remove(queryId)
      if (clientQueryId != null) {
         clientQueryIdToQueryIdMap.remove(clientQueryId)
      }
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
   private fun sendUpdate(metaData: RunningQueryStatus) {
      // TODO : Move the runblocking onto an appropriate dispatcher
      runBlocking {
         queryMetadataSink.emit(metaData)
      }

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

   override fun entryUpdated(event: EntryEvent<String, RunningQueryStatus>) {
      sendUpdate(event.value)
   }

   override fun entryAdded(event: EntryEvent<String, RunningQueryStatus>) {
      sendUpdate(event.value)
   }

}

data class RunningQueryStatus(
   val queryId: String,
   val taxiQlQuery: TaxiQLQueryString? = null,
   val completedProjections: Int = 0,
   val estimatedProjectionCount: Int = 0,
   val startTime: Instant,
   val running: Boolean = true,
   val state: QueryResponse.ResponseStatus = QueryResponse.ResponseStatus.UNKNOWN,
   val queryMode: QueryMode
) : java.io.Serializable

data class QueryCancellationMessage(val queryId: String, val queryClientId: String): java.io.Serializable
