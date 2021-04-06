package io.vyne.queryService.history.db.entity

import io.vyne.query.QueryResponse
import java.time.Instant
import javax.persistence.*


@Entity(name = "query_summary")
data class PersistentQuerySummary(
   @Id
   val queryId: String,
   @Column(unique = true)
   val clientQueryId: String,
   val taxiQl: String?,
   val queryJson: String?,
   val startTime: Instant,
   @Enumerated(EnumType.STRING)
   val responseStatus: QueryResponse.ResponseStatus,
   val endTime: Instant? = null,
   val recordSize: Int? = null
)

@Table(
   indexes = [
      Index(
         name = "ix_queryResultByQueryId",
         columnList = "queryId"
      )
   ]
)
@Entity
data class QueryResultRow(
   @Id
   @GeneratedValue(strategy = GenerationType.AUTO)
   val rowId: Long = 0,
   val queryId: String,
   val json: String
)

@Entity
data class LineageRecord(
   // Data sources must be able to compute a repeatable, consistent id
   // to use for persistence.
   @Id
   val hashId: String,
   val value: String
)
