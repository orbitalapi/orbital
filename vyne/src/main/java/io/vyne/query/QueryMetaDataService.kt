package io.vyne.queryService

import io.vyne.query.QuerySpecTypeNode
import io.vyne.schemas.QualifiedName
import io.vyne.utils.log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

interface QueryMonitor {
   fun reportStart(queryId:String?, clientQueryId:String?)
   fun reportRecords(queryId:String?, records:Int)
   fun reportComplete(queryId:String?)
   fun reportTarget(queryId:String?, target: QuerySpecTypeNode)
   fun queryMetaDataEvents(queryId:String): Flow<QueryMetaData?>
   fun metaDataEvents() : Flow<QueryMetaData?>
}

//TODO refactor reportXX functions
class QueryMetaDataService : QueryMonitor {

   //Query Specific Flow
   private val queryMetadata: ConcurrentHashMap<String, Pair<MutableSharedFlow<QueryMetaData>, SharedFlow<QueryMetaData>>> = ConcurrentHashMap()

   private val _metadata = MutableSharedFlow<QueryMetaData>()
   private val queryEvents = _metadata.asSharedFlow()

   //Map of clientQueryId to actual queryId - allows client to specify handle
   val queryIdToClientQueryIdMap = mutableMapOf<String,String>()
   /**
    * Return a SharedFlow of QueryMetaData events for given queryID creating event flows as necessary
    */
   override fun queryMetaDataEvents(queryId:String):Flow<QueryMetaData> {
      return metadata(queryId)?.second ?: emptyFlow()
   }

   override fun metaDataEvents():Flow<QueryMetaData> {
      return queryEvents
   }

   /**
    * Return latest QueryMetaData from latest state change or UNKNOWN QueryMetaData
    */
   fun queryMetaData(queryId:String?):QueryMetaData? {
      val latestQueryMetaData = metadata(queryId)?.second?.replayCache
      if (queryId == null) {
         return null
      }
      return if (latestQueryMetaData != null && latestQueryMetaData.isNotEmpty()) {
         latestQueryMetaData[0]
      } else {
         QueryMetaData(
            queryId = queryId,
            state = QueryState.UNKNOWN,
            startTime = 0,
            queryMetaDataEventTime = System.currentTimeMillis(),
            recordCount = 0)
      }
   }

   override fun reportStart(queryId:String?, clientQueryId:String?) {
      log().debug("Reporting Query Starting - $queryId")
      queryIdToClientQueryIdMap.putIfAbsent(queryId!!, if (clientQueryId.isNullOrEmpty()) "NO_CLIENT_QUERY" else "$clientQueryId")
      queryMetaData(queryId).let {
         it?.copy(state = QueryState.STARTING, queryMetaDataEventTime = System.currentTimeMillis())
      }.let {
         GlobalScope.launch { notifyMetaData(queryId, it) }
      }
   }

   override fun reportRecords(queryId:String?, records:Int) {
      queryMetaData(queryId).let {
         if (it?.state == QueryState.UNKNOWN) {log().warn("Reporting data received for an unknown query - $it")}
         it?.copy(recordCount = it.recordCount+records, queryMetaDataEventTime = System.currentTimeMillis())
      }.let {
         GlobalScope.launch { notifyMetaData(queryId, it) }
      }
   }

   override fun reportComplete(queryId:String?) {
      log().debug("Reporting Query Finished - $queryId")
      queryMetaData(queryId).let {
         it?.copy(state = QueryState.COMPLETE, queryMetaDataEventTime = System.currentTimeMillis())
      }.let {
         GlobalScope.launch { notifyMetaData(queryId, it) }
      }
   }

   override fun reportTarget(queryId:String?, target: QuerySpecTypeNode) {
      log().debug("Reporting Query Target - $queryId")
      queryMetaData(queryId).let {
         it?.copy(target = target.type.name, queryMetaDataEventTime = System.currentTimeMillis())
      }.let {
         GlobalScope.launch { notifyMetaData(queryId, it) }
      }
   }

   fun reportState(queryId:String?, state:QueryState) {
      queryMetaData(queryId).let {
         if (it?.state == QueryState.UNKNOWN) {log().warn("Reporting data received for an unknown query - $it")}
         it?.copy(state = state, queryMetaDataEventTime = System.currentTimeMillis())
      }.let {
         GlobalScope.launch { notifyMetaData(queryId, it) }
      }
   }

   /**
    * Emit the metadata event for the given queryId
    */
   private suspend fun notifyMetaData(queryId:String?, metaData: QueryMetaData?):QueryMetaData? {
      if (metaData == null) {
         return null
      }
      metadata(queryId)?.first?.emit(metaData)
      _metadata.emit(metaData)
      return metaData
   }

   /**
    * Return pair of mutable stateflow and shared flow for query events for given queryId
    * storing flows in metadata hashmap
    */
   private fun metadata(queryId:String?): Pair<MutableSharedFlow<QueryMetaData>, SharedFlow<QueryMetaData>>?{
      if (queryId == null) {
         return null
      }

      return queryMetadata.computeIfAbsent(queryId) {
         val queryState = MutableSharedFlow<QueryMetaData>(1)
         Pair(queryState, queryState.asSharedFlow())
      }
   }

   companion object MonitorService {
      private val instance = QueryMetaDataService()

      val monitor: QueryMetaDataService
         get() {
            return instance
         }
   }
}

data class QueryMetaData(
   val target: QualifiedName? = null,
   val queryId: String? = null,
   val userQueryId: String? = null,
   val state:QueryState,
   val startTime:Long,
   val queryMetaDataEventTime:Long,
   val recordCount:Int = 0
   )

enum class QueryState {
   UNKNOWN,
   STARTING,
   RUNNING,
   COMPLETE,
   ERROR,
}

