package io.vyne.history

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.models.TypedObject
import io.vyne.models.json.Jackson
import io.vyne.query.QueryCompletedEvent
import io.vyne.query.QueryResponse
import io.vyne.query.QueryResultEvent
import io.vyne.query.RestfulQueryExceptionEvent
import io.vyne.query.RestfulQueryResultEvent
import io.vyne.query.TaxiQlQueryExceptionEvent
import io.vyne.query.TaxiQlQueryResultEvent
import io.vyne.query.history.QuerySummary
import lang.taxi.types.Type
import java.util.UUID

object QueryResultEventMapper {
   private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper
   fun toQuerySummary(event: RestfulQueryResultEvent): QuerySummary {
      return QuerySummary(
         queryId = event.queryId,
         clientQueryId = event.clientQueryId ?: event.queryId,
         taxiQl = null,
         queryJson = objectMapper.writeValueAsString(event.query),
         startTime = event.queryStartTime,
         responseStatus = QueryResponse.ResponseStatus.INCOMPLETE
      )
   }

   fun toQuerySummary(event: TaxiQlQueryResultEvent): QuerySummary {
      val anonymousTypes = if (event.typedInstance.type.taxiType.anonymous && event.typedInstance is TypedObject) {
         val anonymousTypeForQuery =  event.anonymousTypes.firstOrNull { it.taxiType.qualifiedName ==  event.typedInstance.typeName}
         if (anonymousTypeForQuery == null) {
            emptySet<Type>()
         } else {
            setOf(anonymousTypeForQuery)
         }
      } else {
         emptySet<Type>()
      }
     return  QuerySummary(
         queryId = event.queryId,
         clientQueryId = event.clientQueryId ?: UUID.randomUUID().toString(),
         taxiQl = event.query,
         queryJson = null,
         startTime = event.queryStartTime,
         responseStatus = QueryResponse.ResponseStatus.INCOMPLETE,
         anonymousTypesJson = objectMapper.writeValueAsString(anonymousTypes)
      )
   }

   fun toQuerySummary(event: QueryCompletedEvent): QuerySummary {
      return QuerySummary(
         queryId = event.queryId,
         clientQueryId = event.queryId,
         taxiQl = event.query,
         queryJson = objectMapper.writeValueAsString(event.query),
         endTime = event.timestamp,
         responseStatus = QueryResponse.ResponseStatus.COMPLETED,
         startTime = event.timestamp
      )
   }

   fun toQuerySummary(event: RestfulQueryExceptionEvent): QuerySummary {
      return QuerySummary(
         queryId = event.queryId,
         clientQueryId = event.clientQueryId ?: event.queryId,
         taxiQl = null,
         queryJson = objectMapper.writeValueAsString(event.query),
         startTime = event.queryStartTime,
         responseStatus = QueryResponse.ResponseStatus.ERROR
      )
   }

   fun toQuerySummary(event: TaxiQlQueryExceptionEvent): QuerySummary {
      return QuerySummary(
         queryId = event.queryId,
         clientQueryId = event.clientQueryId ?: event.queryId,
         taxiQl = event.query,
         queryJson = null,
         startTime = event.queryStartTime,
         responseStatus = QueryResponse.ResponseStatus.ERROR
      )
   }
}
