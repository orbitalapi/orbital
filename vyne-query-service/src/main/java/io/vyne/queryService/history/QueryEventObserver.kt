package io.vyne.queryService.history

import io.vyne.models.TypedInstance
import io.vyne.query.Query
import io.vyne.query.QueryResponse
import io.vyne.query.QueryResult
import io.vyne.query.active.ActiveQueryMonitor
import io.vyne.queryService.FailedSearchResponse
import io.vyne.schemas.Type
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onErrorResume
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
            }.catch {

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
   suspend fun handleEvent(event: QueryEvent)
}

sealed class QueryEvent

data class RestfulQueryResultEvent(
   val query: Query,
   val queryId: String,
   val clientQueryId: String?,
   val typedInstance: TypedInstance
) : QueryEvent()

data class QueryFailureEvent(
   val queryId: String,
   val clientQueryId: String?,
   val failure: FailedSearchResponse
) : QueryEvent()

data class TaxiQlQueryResultEvent(
   val query: TaxiQLQueryString,
   val queryId: String,
   val clientQueryId: String?,
   val typedInstance: TypedInstance,
   val anonymousTypes: Set<Type>
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
