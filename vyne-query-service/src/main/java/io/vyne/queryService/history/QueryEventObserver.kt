package io.vyne.queryService.history

import io.vyne.models.TypedInstance
import io.vyne.query.Query
import io.vyne.query.QueryResponse
import io.vyne.query.QueryResult
import io.vyne.query.active.ActiveQueryMonitor
import io.vyne.queryService.FailedSearchResponse
import io.vyne.schemas.Type
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import lang.taxi.types.TaxiQLQueryString
import java.time.Instant

/**
 * Takes a queries results, metadata, etc, and streams the out to a QueryHistory provider
 * to be captured.
 */
class QueryEventObserver(private val consumer: QueryEventConsumer, private val activeQueryMonitor:ActiveQueryMonitor) {
   /**
    * Attaches an observer to the result flow of the QueryResponse, returning
    * an updated QueryResponse with it's internal flow updated.
    *
    * Callers should only used the returned value.
    */
   suspend fun responseWithQueryHistoryListener(query: Query, queryResponse: QueryResponse): QueryResponse {
      return when (queryResponse) {
         is QueryResult -> captureQueryResultStreamToHistory(query, queryResponse)
         is FailedSearchResponse -> emitFailure(query, queryResponse)
         else -> error("Received unknown type of QueryResponse: ${queryResponse::class.simpleName}")
      }
   }

   private fun captureQueryResultStreamToHistory(query: Query, queryResult: QueryResult): QueryResult {
      return queryResult.copy(
         results = queryResult.results
            .onEach { typedInstance ->
               activeQueryMonitor.incrementEmittedRecordCount(queryId = queryResult.queryResponseId)
               consumer.handleEvent(
                  RestfulQueryResultEvent(
                     query, queryResult.queryResponseId, queryResult.clientQueryId, typedInstance
                  )
               )
            }
            .onCompletion { error ->
               if (error == null) {
                  consumer.handleEvent(
                     QueryCompletedEvent(
                        queryResult.queryResponseId,
                        Instant.now()
                     )
                  )
               } else {
                  consumer.handleEvent(
                     RestfulQueryExceptionEvent(
                        query,
                        queryResult.queryResponseId,
                        queryResult.clientQueryId,
                        Instant.now(),
                        error.message ?: "No message provided"
                     )
                  )
               }
               activeQueryMonitor.reportComplete(queryResult.queryId)
            }
      )
   }

   private suspend fun emitFailure(query: Query, failure: FailedSearchResponse): FailedSearchResponse {
      consumer.handleEvent(
         QueryFailureEvent(
            failure.queryResponseId,
            failure.clientQueryId,
            failure
         )
      )
      return failure
   }

   /**
    * Attaches an observer to the result flow of the QueryResponse, returning
    * an updated QueryResponse with it's internal flow updated.
    *
    * Callers should only used the returned value.
    */
   suspend fun responseWithQueryHistoryListener(query: TaxiQLQueryString, queryResponse: QueryResponse): QueryResponse {
      return when (queryResponse) {
         is QueryResult -> captureTaxiQlQueryResultStreamToHistory(query, queryResponse)
         is FailedSearchResponse -> emitFailure(query, queryResponse)
         else -> error("Received unknown type of QueryResponse: ${queryResponse::class.simpleName}")
      }
   }

   private fun captureTaxiQlQueryResultStreamToHistory(
      query: TaxiQLQueryString,
      queryResult: QueryResult
   ): QueryResult {
      return queryResult.copy(
         results = queryResult.results

            .onEach { typedInstance ->
               activeQueryMonitor.incrementEmittedRecordCount(queryId = queryResult.queryResponseId)
               consumer.handleEvent(
                  TaxiQlQueryResultEvent(
                     query,
                     queryResult.queryResponseId,
                     queryResult.clientQueryId,
                     typedInstance,
                     queryResult.anonymousTypes
                  )
               )
            }
            .onCompletion { error ->
               if (error == null) {
                  consumer.handleEvent(
                     QueryCompletedEvent(
                        queryResult.queryResponseId,
                        Instant.now()
                     )
                  )
               } else {
                  consumer.handleEvent(
                     TaxiQlQueryExceptionEvent(
                        query,
                        queryResult.queryResponseId,
                        queryResult.clientQueryId,
                        Instant.now(),
                        error.message ?: "No message provided"
                     )
                  )
               }
               activeQueryMonitor.reportComplete(queryResult.queryId)
            }
      )

   }

   private suspend fun emitFailure(query: TaxiQLQueryString, failure: FailedSearchResponse): FailedSearchResponse {
      consumer.handleEvent(
         QueryFailureEvent(
            failure.queryResponseId,
            failure.clientQueryId,
            failure
         )
      )
      return failure
   }
}

interface QueryEventConsumer {
   fun handleEvent(event: QueryEvent): Job
}

sealed class QueryEvent

data class RestfulQueryResultEvent(
   val query: Query,
   override val queryId: String,
   override val clientQueryId: String?,
   override val typedInstance: TypedInstance
) :  QueryResultEvent, QueryEvent() {
   override val anonymousTypes: Set<Type> = emptySet()
}

data class QueryFailureEvent(
   val queryId: String,
   val clientQueryId: String?,
   val failure: FailedSearchResponse
) : QueryEvent()

data class TaxiQlQueryResultEvent(
   val query: TaxiQLQueryString,
   override val queryId: String,
   override val clientQueryId: String?,
   override val typedInstance: TypedInstance,
   override val anonymousTypes: Set<Type>
) : QueryResultEvent, QueryEvent()

interface QueryResultEvent {
   val queryId: String
   val clientQueryId: String?
   val typedInstance: TypedInstance
   val anonymousTypes: Set<Type>
}

data class QueryCompletedEvent(
   val queryId: String,
   val timestamp: Instant
) : QueryEvent()

data class TaxiQlQueryExceptionEvent(
   val query: TaxiQLQueryString,
   val queryId: String,
   val clientQueryId: String?,
   val timestamp: Instant,
   val message: String
) : QueryEvent()

data class RestfulQueryExceptionEvent(
   val query: Query,
   val queryId: String,
   val clientQueryId: String?,
   val timestamp: Instant,
   val message: String
) : QueryEvent()
