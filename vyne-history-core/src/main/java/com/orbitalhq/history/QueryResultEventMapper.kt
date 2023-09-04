package com.orbitalhq.history

import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.json.Jackson
import com.orbitalhq.query.QueryCompletedEvent
import com.orbitalhq.query.QueryResponse
import com.orbitalhq.query.QueryResultEvent
import com.orbitalhq.query.QueryStartEvent
import com.orbitalhq.query.RestfulQueryExceptionEvent
import com.orbitalhq.query.RestfulQueryResultEvent
import com.orbitalhq.query.StreamingQueryCancelledEvent
import com.orbitalhq.query.TaxiQlQueryExceptionEvent
import com.orbitalhq.query.TaxiQlQueryResultEvent
import com.orbitalhq.query.history.QuerySummary
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

   fun toQuerySummary(event: StreamingQueryCancelledEvent): QuerySummary {
      return QuerySummary(
         queryId = event.queryId,
         clientQueryId = event.clientQueryId ?: event.queryId,
         taxiQl = event.query,
         queryJson = null,
         startTime = event.queryStartTime,
         responseStatus = QueryResponse.ResponseStatus.CANCELLED
      )
   }

   fun toQuerySummary(event: QueryStartEvent): QuerySummary {
     return QuerySummary(
         queryId = event.queryId,
         clientQueryId = event.clientQueryId,
         taxiQl = event.taxiQuery,
         queryJson = event.query?.let { objectMapper.writeValueAsString(event.query)  } ,
         responseStatus = QueryResponse.ResponseStatus.RUNNING,
         startTime = event.timestamp,
         responseType = event.message
      )
   }
}
