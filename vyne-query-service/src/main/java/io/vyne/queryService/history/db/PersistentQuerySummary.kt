package io.vyne.queryService.history.db

import com.fasterxml.jackson.annotation.JsonRawValue
import io.vyne.query.QueryResponse
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.relational.core.mapping.Table
import java.time.Duration
import java.time.Instant


@Table("query_summary")
data class PersistentQuerySummary(
   val queryId: String,
   val clientQueryId: String,
   val taxiQl: String?,
   // Note - attempts to use the actual object here (rather than the
   // json) have failed.  Looks like r2dbc support for column-level
   // mappers is still too young.
   @JsonRawValue
   val queryJson: String?,
   val startTime: Instant,
   val responseStatus: QueryResponse.ResponseStatus,
   val endTime: Instant? = null,
   val recordSize: Int? = null,
   val errorMessage: String? = null,
   // r2dbc requires an id, which can be set during persistence
   // in order to determine if the row exists
   @Id
   val id: Long? = null,
) {
   @Transient
   val durationMs = endTime?.let { Duration.between(startTime, endTime).toMillis() }
}

@Table
data class QueryResultRow(
   @Id
   val rowId: Long? = null,
   val queryId: String,
   @JsonRawValue
   val json: String
)

@Table
data class LineageRecord(
   // Data sources must be able to compute a repeatable, consistent id
   // to use for persistence.
   @Id
   val hashId: String,
   val value: String
)
