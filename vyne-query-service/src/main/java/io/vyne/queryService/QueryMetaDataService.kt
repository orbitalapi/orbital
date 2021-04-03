package io.vyne.queryService

import io.vyne.utils.log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class QueryMetaDataService {

   private val metadata: ConcurrentHashMap<String, Pair<MutableSharedFlow<QueryMetaData>, SharedFlow<QueryMetaData>>> = ConcurrentHashMap()

   /**
    * Return a SharedFlow of QueryMetaData events for given queryID creating event flows as necessary
    */
   fun queryMetaDataEvents(queryId:String):SharedFlow<QueryMetaData?> {
      return metadata(queryId).second
   }

   /**
    * Return latest QueryMetaData from latest state change or UNKNOWN QueryMetaData
    */
   fun queryMetaData(queryId:String):QueryMetaData {
      val latestQueryMetaData = metadata(queryId).second.replayCache
      return if (latestQueryMetaData.isNotEmpty()) {
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

   //TODO refactor reportXX functions
   fun reportRecords(queryId:String, records:Int) {
      queryMetaData(queryId).let {
         if (it.state == QueryState.UNKNOWN) {log().warn("Reporting data received for an unknown query - $it")}
         it.copy(recordCount = it.recordCount+records, queryMetaDataEventTime = System.currentTimeMillis())
      }.let {
         GlobalScope.launch { _notifyMetaData(queryId, it) }
      }
   }

   fun reportState(queryId:String, state:QueryState) {
      queryMetaData(queryId).let {
         if (it.state == QueryState.UNKNOWN) {log().warn("Reporting data received for an unknown query - $it")}
         it.recordCount
         it.copy(state = state, queryMetaDataEventTime = System.currentTimeMillis())
      }.let {
         GlobalScope.launch { _notifyMetaData(queryId, it) }
      }
   }

   /**
    *
    */
   private suspend fun _notifyMetaData(queryId:String, metaData: QueryMetaData):QueryMetaData {
      val metaData = QueryMetaData(
         queryId= queryId,
         state = QueryState.STARTING,
         startTime = System.currentTimeMillis(),
         queryMetaDataEventTime = System.currentTimeMillis(),
         recordCount = 0)
      metadata(queryId).first.emit(metaData)
      return metaData
   }

   /**
    * Return pair of mutable stateflow and shared flow for query events for given queryId
    * storing flows in metadata hashmap
    */
   private fun metadata(queryId:String): Pair<MutableSharedFlow<QueryMetaData>, SharedFlow<QueryMetaData>>{
      return metadata.computeIfAbsent(queryId) {
         val queryState = MutableSharedFlow<QueryMetaData>(1)
         Pair(queryState, queryState.asSharedFlow())
      }
   }
}

data class QueryMetaData(
   val queryId: String,
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

