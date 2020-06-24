package io.vyne.queryService.persistency.entity

import io.vyne.queryService.QueryHistoryRecord
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant


@Table("query_history_records")
data class QueryHistoryRecordEntity(
   @Id
   @Column("id")
   val id: Long? = null,
   @Column("query_id")
   val queryId: String,
   @Column("record")
   val record: QueryHistoryRecord<out Any>,
   @Column("executed_at")
   val timestamp: Instant
)
