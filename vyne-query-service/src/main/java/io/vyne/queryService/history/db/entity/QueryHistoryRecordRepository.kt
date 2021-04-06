package io.vyne.queryService.history.db.entity

import io.vyne.query.QueryResponse
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.data.repository.query.Param
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

interface QueryHistoryRecordRepository : R2dbcRepository<PersistentQuerySummary, Long> {

   @Modifying
   @Query(
      "update query_summary r set r.end_time = :endTime,  r.response_status = :status, r.error_message = :errorMessage where r.query_id = :queryId"
   )
   fun setQueryEnded(
      @Param("queryId") queryId: String,
      @Param("endTime") endTime: Instant,
      @Param("status") status: QueryResponse.ResponseStatus,
      @Param("errorMessage") message: String? = null
   ): Mono<Void>

   fun findByQueryId(queryId: String): Mono<PersistentQuerySummary>
   fun findByClientQueryId(queryId: String): Mono<PersistentQuerySummary>
//   fun findByIdNotNull(page: Pageable): Flux<PersistentQuerySummary>
}

interface QueryResultRowRepository : R2dbcRepository<QueryResultRow, Long> {
   // TODO : This could be big, and returning everything
   fun findAllByQueryId(queryId: String): Flux<QueryResultRow>
}

interface LineageRecordRepository : R2dbcRepository<LineageRecord, String>
