package com.orbitalhq.history.db

import com.orbitalhq.query.history.QueryResultRow
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional

interface QueryResultRowRepository : JpaRepository<QueryResultRow, Long> {
   // TODO : This could be big, and returning everything
   @Transactional
   fun findAllByQueryId(queryId: String): List<QueryResultRow>

   // TODO : When coding this, it seems we're getting multple results, which
   // shoulnd't be possible  -- will investigate, promise.
   @Transactional
   fun findByQueryIdAndValueHash(queryId: String, valueHash: Int): List<QueryResultRow>

   @Transactional
   fun countAllByQueryId(queryId: String): Int
}
