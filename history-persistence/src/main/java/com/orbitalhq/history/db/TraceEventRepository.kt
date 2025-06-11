package com.orbitalhq.history.db

import com.orbitalhq.query.history.TraceEventRow
import org.springframework.data.jpa.repository.JpaRepository

interface TraceEventRepository : JpaRepository<TraceEventRow, String> {
}
