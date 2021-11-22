package io.vyne.history.db

import io.vyne.query.QueryResponse
import io.vyne.query.history.LineageRecord
import io.vyne.query.history.QueryResultRow
import io.vyne.query.history.QuerySankeyChartRow
import io.vyne.query.history.QuerySummary
import io.vyne.query.history.RemoteCallResponse
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

interface QueryHistoryRecordRepository : JpaRepository<QuerySummary, Long> {

   // Setting flushAutomatically and clearAUtomatically as without these,
   // tests fail.  (Note - calling repository.flush() didn't solve the issue in tests).
   // Need to understand if this causes an overall performance
   // issue.
   @Modifying(flushAutomatically = true, clearAutomatically = true)
   @Query(
      "update QUERY_SUMMARY r set r.endTime = :endTime, r.responseStatus = :status, r.errorMessage = :errorMessage, r.recordCount = :recordCount where r.queryId = :queryId"
   )
   @Transactional
   fun setQueryEnded(
      @Param("queryId") queryId: String,
      @Param("endTime") endTime: Instant,
      @Param("status") status: QueryResponse.ResponseStatus,
      @Param("recordCount") recordCount: Int,
      @Param("errorMessage") message: String? = null
   ): Int

   @Transactional
   fun findByQueryId(queryId: String): QuerySummary

   @Transactional
   fun findByClientQueryId(queryId: String): QuerySummary

   @Transactional
   fun findAllByOrderByStartTimeDesc(pageable: Pageable): List<QuerySummary>
   fun findAllByResponseType(responseType: String): List<QuerySummary>
}

interface QueryResultRowRepository : JpaRepository<QueryResultRow, Long> {
   // TODO : This could be big, and returning everything
   // Does r2dbc support pagination?
   @Transactional
   fun findAllByQueryId(queryId: String): List<QueryResultRow>

   // TODO : When coding this, it seems we're getting multple results, which
   // shoulnd't be possible  -- will investigate, promise.
   @Transactional
   fun findByQueryIdAndValueHash(queryId: String, valueHash: Int): List<QueryResultRow>
   @Transactional
   fun countAllByQueryId(queryId: String): Int
}

interface LineageRecordRepository : JpaRepository<LineageRecord, String> {

   @Transactional
   fun findAllByQueryIdAndDataSourceType(queryId: String, dataSourceType: String): List<LineageRecord>

   @Transactional
   fun findAllByQueryId(queryId: String): List<LineageRecord>
   @Modifying
   @Query(
      value = "INSERT INTO LINEAGE_RECORD (data_source_id, query_id, data_source_type, data_source_json) VALUES(:dataSourceId, :queryId, :dataSourceType, :dataSourceJson) ON CONFLICT DO NOTHING",
      nativeQuery = true
   )
   @Transactional
   fun upsertLineageRecord(
      @Param("dataSourceId") dataSourceId: String,
      @Param("queryId") queryId: String,
      @Param("dataSourceType") dataSourceType: String,
      @Param("dataSourceJson") dataSourceJson: String
   )
}

/**
 *
 *    @Id

val dataSourceId: String,
val queryId: String,
val dataSourceType: String,
val dataSourceJson: String
 */

interface RemoteCallResponseRepository : JpaRepository<RemoteCallResponse, String> {
   @Transactional
   fun findAllByQueryId(queryId: String): List<RemoteCallResponse>
   @Transactional
   fun findAllByRemoteCallId(remoteCallId: String): List<RemoteCallResponse>
   @Modifying
   @Query(
      value ="INSERT INTO REMOTE_CALL_RESPONSE VALUES(:responseId, :remoteCallId, :queryId, :response) ON CONFLICT DO NOTHING",
      nativeQuery = true
   )
   @Transactional
   fun upsertRemoteCallResponse(
      @Param("responseId") responseId: String,
      @Param("remoteCallId") remoteCallId: String,
      @Param("queryId") queryId: String,
      @Param("response") response: String)
}


interface QuerySankeyChartRowRepository : JpaRepository<QuerySankeyChartRow, Long> {
   fun findAllByQueryId(queryId: String): List<QuerySankeyChartRow>
}

