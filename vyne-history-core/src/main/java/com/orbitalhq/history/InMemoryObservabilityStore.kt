package com.orbitalhq.history

import com.orbitalhq.query.history.LineageRecord
import com.orbitalhq.query.history.QueryResultRow
import com.orbitalhq.query.history.RemoteCallResponse


/**
 * Holds all captured events in memory.
 *
 * Be aware that results can be large, so
 * you should not have a long-lived instance of this.
 *
 * Useful for tests, creating/working with stubs,
 * or playground environments, but should not be used
 * for production, as will create OOM errors
 */
class InMemoryObservabilityStore : QueryObservabilityWriter {
   private val _resultRows = mutableListOf<QueryResultRow>()
   private val _remoteCallResponses = mutableListOf<RemoteCallResponse>()
   private val _lineageRecords = mutableListOf<LineageRecord>()
   override fun storeResultRow(resultRow: QueryResultRow) {
      _resultRows.add(resultRow)
   }

   override fun storeRemoteCallResponse(remoteCallResponse: RemoteCallResponse) {
      _remoteCallResponses.add(remoteCallResponse)
   }

   override fun storeLineageRecord(lineageRecord: LineageRecord) {
      _lineageRecords.add(lineageRecord)
   }

   val resultRows: List<QueryResultRow>
      get() {
         return _resultRows.toList()
      }

   val remoteCallResponses: List<RemoteCallResponse>
      get() {
         return _remoteCallResponses.toList()
      }

   val lineageRecords: List<LineageRecord>
      get() {
         return _lineageRecords.toList()
      }


}
