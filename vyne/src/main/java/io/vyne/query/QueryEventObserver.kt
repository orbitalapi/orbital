package io.vyne.query

import io.vyne.models.TypedInstance
import io.vyne.schemas.Type
import lang.taxi.types.TaxiQLQueryString
import java.time.Instant

interface QueryEventConsumer : RemoteCallOperationResultHandler {
   fun handleEvent(event: QueryEvent)
}

sealed class QueryEvent

data class VyneQueryStatisticsEvent(val vyneQueryStatistics: VyneQueryStatistics): QueryEvent()

data class RestfulQueryResultEvent(
   val query: Query,
   override val queryId: String,
   override val clientQueryId: String?,
   override val typedInstance: TypedInstance,
   override val queryStartTime: Instant
) : QueryResultEvent, QueryEvent() {
   override val anonymousTypes: Set<Type> = emptySet()
}

data class QueryFailureEvent(
   val queryId: String,
   val clientQueryId: String?,
   val failure: FailedQueryResponse
) : QueryEvent()

data class TaxiQlQueryResultEvent(
   val query: TaxiQLQueryString,
   override val queryId: String,
   override val clientQueryId: String?,
   override val typedInstance: TypedInstance,
   override val anonymousTypes: Set<Type>,
   override val queryStartTime: Instant
) : QueryResultEvent, QueryEvent()

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
) : QueryEvent()

data class TaxiQlQueryExceptionEvent(
   val query: TaxiQLQueryString,
   val queryId: String,
   val clientQueryId: String?,
   val timestamp: Instant,
   val message: String,
   val queryStartTime: Instant,
   val recordCount: Int = 0
) : QueryEvent()

data class RestfulQueryExceptionEvent(
   val query: Query,
   val queryId: String,
   val clientQueryId: String?,
   val timestamp: Instant,
   val message: String,
   val queryStartTime: Instant,
   val recordCount: Int = 0
) : QueryEvent()

data class QueryStartEvent(
   val queryId: String,
   val timestamp: Instant,
   val taxiQuery: TaxiQLQueryString?,
   val query: Query?,
   val clientQueryId: String?,
   val message: String
) : QueryEvent()

