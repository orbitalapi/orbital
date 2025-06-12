package com.orbitalhq.history.db.tracing

import com.orbitalhq.query.history.tracing.TraceEventRow
import org.springframework.data.jpa.repository.JpaRepository

interface TraceEventRepository : JpaRepository<TraceEventRow, String> {
   fun findByQueryIdOrderByTimestampAsc(queryId: String): List<TraceEventRow>
}
