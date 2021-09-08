package io.vyne.history.db

import io.vyne.query.QueryResponse
import io.vyne.query.history.LineageRecord
import io.vyne.query.history.QueryResultRow
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
   ):Int

   fun findByQueryId(queryId: String): QuerySummary
   fun findByClientQueryId(queryId: String): QuerySummary

   fun findAllByOrderByStartTimeDesc(pageable: Pageable): List<QuerySummary>
}

interface QueryResultRowRepository : JpaRepository<QueryResultRow, Long> {
   // TODO : This could be big, and returning everything
   // Does r2dbc support pagination?
   fun findAllByQueryId(queryId: String): List<QueryResultRow>

   // TODO : When coding this, it seems we're getting multple results, which
   // shoulnd't be possible  -- will investigate, promise.
   fun findByQueryIdAndValueHash(queryId: String, valueHash: Int): List<QueryResultRow>
   fun countAllByQueryId(queryId: String): Int
}

interface LineageRecordRepository : JpaRepository<LineageRecord, String> {

   fun findAllByQueryIdAndDataSourceType(queryId: String, dataSourceType: String): List<LineageRecord>

   fun findAllByQueryId(queryId: String): List<LineageRecord>
}

interface RemoteCallResponseRepository : JpaRepository<RemoteCallResponse, String> {
   fun findAllByQueryId(queryId: String): List<RemoteCallResponse>
   fun findAllByRemoteCallId(remoteCallId: String): List<RemoteCallResponse>
}
