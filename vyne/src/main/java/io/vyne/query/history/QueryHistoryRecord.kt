package io.vyne.query.history

import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.vyne.query.HistoryQueryResponse
import io.vyne.query.Query
import java.time.Instant

@JsonTypeInfo(
   use = JsonTypeInfo.Id.CLASS,
   include = JsonTypeInfo.As.PROPERTY,
   property = "className")
interface QueryHistoryRecord<T> {
   val query: T
   val response: HistoryQueryResponse
   val timestamp: Instant
   val id: String
      get() {
         return response.queryResponseId
      }

   fun withResponse(response: HistoryQueryResponse): QueryHistoryRecord<T>
}

data class VyneQlQueryHistoryRecord(
   override val query: String,
   override val response: HistoryQueryResponse,
   override val timestamp: Instant = Instant.now()
) : QueryHistoryRecord<String> {
   override fun withResponse(historyQueryResponse: HistoryQueryResponse): QueryHistoryRecord<String> {
      return copy(response = historyQueryResponse)
   }
}

data class RestfulQueryHistoryRecord(
   override val query: Query,
   override val response: HistoryQueryResponse,
   override val timestamp: Instant = Instant.now()
) : QueryHistoryRecord<Query> {
   override fun withResponse(historyQueryResponse: HistoryQueryResponse): QueryHistoryRecord<Query> {
      return copy(response = historyQueryResponse)
   }
}
