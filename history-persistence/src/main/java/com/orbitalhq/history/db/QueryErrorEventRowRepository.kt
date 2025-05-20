package com.orbitalhq.history.db

import com.orbitalhq.query.history.QueryErrorEventRow
import com.orbitalhq.query.history.QueryResultRow
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional

interface QueryErrorEventRowRepository : JpaRepository<QueryErrorEventRow, Long> {
   @Transactional
   fun findAllByQueryId(queryId: String): List<QueryErrorEventRow>

   @Transactional
   fun countAllByQueryId(queryId: String): Int
}
