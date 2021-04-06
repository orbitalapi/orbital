package io.vyne.queryService.history.db.entity

import io.r2dbc.spi.Row
import io.vyne.query.Query
import io.vyne.query.QueryResponse
import org.springframework.core.convert.converter.Converter
import org.springframework.data.annotation.Id
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.r2dbc.mapping.OutboundRow
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant


@Table("query_summary")
data class PersistentQuerySummary(
   val queryId: String,
   val clientQueryId: String,
   val taxiQl: String?,
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
)

@Table
data class QueryResultRow(
   @Id
   val rowId: Long? = null,
   val queryId: String,
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

//
//class QuerySpecTypeNodeConverter(private val mapper: ObjectMapper = Jackson.defaultObjectMapper) :
//
//   override fun convertToDatabaseColumn(attribute: Query?): String? {
//      return attribute?.let { mapper.writeValueAsString(attribute) }
//   }
//
//   override fun convertToEntityAttribute(dbData: String?): Query? {
//      return dbData?.let { mapper.readValue(it) }
//   }
//
//}
@WritingConverter
class WritingQueryConverter : Converter<Query, OutboundRow> {
   override fun convert(source: Query?): OutboundRow? {
      TODO("Not yet implemented")
   }
}

@ReadingConverter
class ReadingQueryConverter : Converter<Row, Query> {
   override fun convert(source: Row): Query? {
      TODO("Not yet implemented")
   }

}
