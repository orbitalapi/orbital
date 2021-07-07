package io.vyne.queryService.history.db

import io.vyne.query.QueryResponse
import io.vyne.query.history.LineageRecord
import io.vyne.query.history.QueryResultRow
import io.vyne.query.history.QuerySummary
import io.vyne.query.history.RemoteCallResponse
import org.springframework.data.domain.Pageable
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface QueryHistoryRecordRepository : CrudRepository<QuerySummary, Long> {

   @Modifying
   @Query(
      "update QUERY_SUMMARY r set r.end_time = :endTime, r.response_status = :status, r.error_message = :errorMessage where r.query_id = :queryId"
   )
   fun setQueryEnded(
      @Param("queryId") queryId: String,
      @Param("endTime") endTime: Instant,
      @Param("status") status: QueryResponse.ResponseStatus,
      @Param("errorMessage") message: String? = null
   ):Int

   fun findByQueryId(queryId: String): QuerySummary
   fun findByClientQueryId(queryId: String): QuerySummary

   fun findAllByOrderByStartTimeDesc(pageable: Pageable): List<QuerySummary>
}

interface QueryResultRowRepository : CrudRepository<QueryResultRow, Long> {
   // TODO : This could be big, and returning everything
   // Does r2dbc support pagination?
   fun findAllByQueryId(queryId: String): List<QueryResultRow>

   // TODO : When coding this, it seems we're getting multple results, which
   // shoulnd't be possible  -- will investigate, promise.
   fun findByQueryIdAndValueHash(queryId: String, valueHash: Int): List<QueryResultRow>
   fun countAllByQueryId(queryId: String): Int
}

interface LineageRecordRepository : CrudRepository<LineageRecord, String> {

   fun findAllByQueryIdAndDataSourceType(queryId: String, dataSourceType: String): List<LineageRecord>

   fun findAllByQueryId(queryId: String): List<LineageRecord>
}

interface RemoteCallResponseRepository : CrudRepository<RemoteCallResponse, String> {
   fun findAllByQueryId(queryId: String): List<RemoteCallResponse>
   fun findAllByRemoteCallId(remoteCallId: String): List<RemoteCallResponse>
}
