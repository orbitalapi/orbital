package io.vyne.queryService.history

import io.vyne.models.TypedInstance
import io.vyne.query.Query
import io.vyne.query.QueryResponse
import io.vyne.query.QueryResult
import io.vyne.queryService.FailedSearchResponse
import io.vyne.vyneql.TaxiQlQueryString
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import java.time.Instant

/**
 * Takes a queries results, metadata, etc, and streams the out to a QueryHistory provider
 * to be captured.
 */
class QueryEventObserver(private val consumer: QueryEventConsumer) {
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
               consumer.handleEvent(
                  RestfulQueryResultEvent(
                     query, queryResult.queryResponseId, queryResult.clientQueryId, typedInstance
                  )
               )
            }
      )
   }

   private suspend fun emitFailure(query: Query, failure: FailedSearchResponse): FailedSearchResponse {
      consumer.handleEvent(
         RestfulQueryFailureEvent(
            query,
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
   suspend fun responseWithQueryHistoryListener(query: TaxiQlQueryString, queryResponse: QueryResponse): QueryResponse {
      return when (queryResponse) {
         is QueryResult -> captureTaxiQlQueryResultStreamToHistory(query, queryResponse)
         is FailedSearchResponse -> emitFailure(query, queryResponse)
         else -> error("Received unknown type of QueryResponse: ${queryResponse::class.simpleName}")
      }
   }

   private fun captureTaxiQlQueryResultStreamToHistory(
      query: TaxiQlQueryString,
      queryResult: QueryResult
   ): QueryResult {
      return queryResult.copy(
         results = queryResult.results
            .onEach { typedInstance ->
               consumer.handleEvent(
                  TaxiQlQueryResultEvent(
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
                     QueryExceptionEvent(
                        queryResult.queryResponseId,
                        Instant.now(),
                        error.message ?: "No message provided"
                     )
                  )
               }
            }
      )

   }

   private suspend fun emitFailure(query: TaxiQlQueryString, failure: FailedSearchResponse): FailedSearchResponse {
      consumer.handleEvent(
         TaxiQlQueryFailureEvent(
            query,
            failure.queryResponseId,
            failure.clientQueryId,
            failure
         )
      )
      return failure
   }
}

interface QueryEventConsumer {
   suspend fun handleEvent(event: QueryEvent)
}

sealed class QueryEvent

data class RestfulQueryResultEvent(
   val query: Query,
   val queryId: String,
   val clientQueryId: String?,
   val typedInstance: TypedInstance
) : QueryEvent()

data class RestfulQueryFailureEvent(
   val query: Query,
   val queryId: String,
   val clientQueryId: String?,
   val failure: FailedSearchResponse
) : QueryEvent()

data class TaxiQlQueryResultEvent(
   val query: TaxiQlQueryString,
   val queryId: String,
   val clientQueryId: String?,
   val typedInstance: TypedInstance
) : QueryEvent()


data class TaxiQlQueryFailureEvent(
   val query: TaxiQlQueryString,
   val queryId: String,
   val clientQueryId: String?,
   val failure: FailedSearchResponse
) : QueryEvent()

data class QueryCompletedEvent(
   val queryId: String,
   val timestamp: Instant
) : QueryEvent()

data class QueryExceptionEvent(
   val queryId: String,
   val timestamp: Instant,
   val message: String
) : QueryEvent()
