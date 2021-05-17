package io.vyne.queryService.history.db

import io.vyne.query.QueryResponse
import io.vyne.query.history.LineageRecord
import io.vyne.query.history.QueryResultRow
import io.vyne.query.history.QuerySummary
import io.vyne.query.history.RemoteCallResponse
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.data.repository.query.Param
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

interface QueryHistoryRecordRepository : R2dbcRepository<QuerySummary, Long> {

   @Modifying
   @Query(
      "update query_summary r set r.end_time = :endTime, r.response_status = :status, r.error_message = :errorMessage where r.query_id = :queryId"
   )
   fun setQueryEnded(
      @Param("queryId") queryId: String,
      @Param("endTime") endTime: Instant,
      @Param("status") status: QueryResponse.ResponseStatus,
      @Param("errorMessage") message: String? = null
   ): Mono<Void>

   fun findByQueryId(queryId: String): Mono<QuerySummary>
   fun findByClientQueryId(queryId: String): Mono<QuerySummary>

   fun findAllByOrderByStartTimeDesc(): Flux<QuerySummary>

}

interface QueryResultRowRepository : R2dbcRepository<QueryResultRow, Long> {
   // TODO : This could be big, and returning everything
   // Does r2dbc support pagination?
   fun findAllByQueryId(queryId: String): Flux<QueryResultRow>

   // TODO : When coding this, it seems we're getting multple results, which
   // shoulnd't be possible  -- will investigate, promise.
   fun findByQueryIdAndValueHash(queryId: String, valueHash: Int): Flux<QueryResultRow>
   fun countAllByQueryId(queryId: String): Mono<Int>
}

interface LineageRecordRepository : R2dbcRepository<LineageRecord, String> {

   fun findAllByQueryIdAndDataSourceType(queryId: String, dataSourceType: String): Flux<LineageRecord>

   fun findAllByQueryId(queryId: String): Flux<LineageRecord>
}

interface RemoteCallResponseRepository : R2dbcRepository<RemoteCallResponse, String> {
   fun findAllByQueryId(queryId: String): Flux<RemoteCallResponse>
   fun findAllByRemoteCallId(remoteCallId: String): Flux<RemoteCallResponse>
}
