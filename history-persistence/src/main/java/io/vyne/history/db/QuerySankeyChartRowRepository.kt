package io.vyne.history.db

import io.vyne.query.history.QuerySankeyChartRow
import io.vyne.query.history.SankeyChartRowId
import org.springframework.data.jpa.repository.JpaRepository

interface QuerySankeyChartRowRepository : JpaRepository<QuerySankeyChartRow, SankeyChartRowId> {
   fun findAllByQueryId(queryId: String): List<QuerySankeyChartRow>
}
