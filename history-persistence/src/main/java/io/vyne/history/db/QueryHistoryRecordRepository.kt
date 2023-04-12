package io.vyne.history.db

import io.vyne.query.QueryResponse
import io.vyne.query.history.QuerySummary
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

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
   fun findByClientQueryId(queryId: String): QuerySummary?

   @Transactional
   fun findAllByOrderByStartTimeDesc(pageable: Pageable): List<QuerySummary>
   fun findAllByResponseType(responseType: String): List<QuerySummary>
}


