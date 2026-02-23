package com.orbitalhq.query

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schemas.Type
import lang.taxi.query.TaxiQLQueryString
import java.time.Instant

interface QueryEventConsumer : RemoteCallOperationResultHandler {
   fun handleEvent(event: QueryEvent)
   fun shutdown() {}

   fun captureQueryStart(
      queryId: String,
      timestamp: Instant,
      taxiQuery: TaxiQLQueryString?,
      query: Query?,
      clientQueryId: String,
      message: String,
      anonymousTypes: Set<Type>
   ) {
      handleEvent(
         QueryStartEvent(
            queryId = queryId,
            timestamp = timestamp,
            taxiQuery = taxiQuery,
            query = query,
            clientQueryId = clientQueryId,
            message = message,
            anonymousTypes = anonymousTypes
         )
      )
   }
}

sealed class QueryEvent(val isTerminalEvent: Boolean)

data class RestfulQueryResultEvent(
   val query: Query,
   override val queryId: String,
   override val clientQueryId: String?,
   override val typedInstance: TypedInstance,
   override val queryStartTime: Instant
) : QueryResultEvent, QueryEvent(isTerminalEvent = false) {
   override val anonymousTypes: Set<Type> = emptySet()
}

data class QueryFailureEvent(
   val queryId: String,
   val clientQueryId: String?,
   val failure: FailedQueryResponse
) : QueryEvent(isTerminalEvent = true)

/**
 * An event captured from the error stream of a query, wrapped for
 * persistence
 */
data class QueryErrorStreamEvent(
   val queryId: String,
   val clientQueryId: String?,
   val event: QueryErrorEvent
): QueryEvent(isTerminalEvent = false)

data class TaxiQlQueryResultEvent(
   val query: TaxiQLQueryString,
   override val queryId: String,
   override val clientQueryId: String?,
   override val typedInstance: TypedInstance,
   override val anonymousTypes: Set<Type>,
   override val queryStartTime: Instant
) : QueryResultEvent, QueryEvent(isTerminalEvent = false)

interface QueryResultEvent {
   val queryId: String
   val clientQueryId: String?
   val typedInstance: TypedInstance
   val anonymousTypes: Set<Type>

   // We need the queryStartTime as we create the query record on the first emitted
   // result.
   val queryStartTime: Instant
}

data class QueryCompletedEvent(
   val queryId: String,
   val timestamp: Instant,
   val query: TaxiQLQueryString,
   val clientQueryId: String?,
   val message: String,
   val recordCount: Int = 0
) : QueryEvent(isTerminalEvent = true)

data class TaxiQlQueryExceptionEvent(
   val query: TaxiQLQueryString,
   val queryId: String,
   val clientQueryId: String?,
   val timestamp: Instant,
   val message: String,
   val queryStartTime: Instant,
   val recordCount: Int = 0
) : QueryEvent(isTerminalEvent = true)

data class StreamingQueryCancelledEvent(val query: TaxiQLQueryString,
                                        val queryId: String,
                                        val clientQueryId: String?,
                                        val timestamp: Instant,
                                        val message: String,
                                        val queryStartTime: Instant,
                                        val recordCount: Int = 0
) : QueryEvent(isTerminalEvent = true)

data class RestfulQueryExceptionEvent(
   val query: Query,
   val queryId: String,
   val clientQueryId: String?,
   val timestamp: Instant,
   val message: String,
   val queryStartTime: Instant,
   val recordCount: Int = 0
) : QueryEvent(isTerminalEvent = true)

data class QueryStartEvent(
   val queryId: String,
   val timestamp: Instant,
   val taxiQuery: TaxiQLQueryString?,
   val query: Query?,
   val clientQueryId: String,
   val message: String,
   val anonymousTypes: Set<Type>,
   val persistResults: Boolean? = null,
   val persistRemoteCallResponses: Boolean? = null,
   val persistRemoteCallMetadata: Boolean? = null,
   val persistTraceEvents: Boolean? = null,
   val persistErrors: Boolean? = null
) : QueryEvent(isTerminalEvent = false)

