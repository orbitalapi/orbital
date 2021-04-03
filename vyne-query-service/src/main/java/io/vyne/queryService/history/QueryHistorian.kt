package io.vyne.queryService.history

import io.vyne.models.TypedInstance
import io.vyne.query.Query
import io.vyne.query.QueryResponse
import io.vyne.query.QueryResult
import io.vyne.queryService.FailedSearchResponse
import io.vyne.vyneql.TaxiQlQueryString
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onEach
import org.springframework.stereotype.Component

/**
 * Takes a queries - results, metadata, etc, and streams the out to a QueryHistory provider
 * to be captured.
 */
@Component
class QueryHistorian {

   private val queryEventFlow = MutableSharedFlow<QueryEvent>()

   suspend fun captureQueryHistory(query: Query, queryResponse: QueryResponse) {
      when (queryResponse) {
         is QueryResult -> captureQueryResultStreamToHistory(query, queryResponse)
         is FailedSearchResponse -> emitFailure(query, queryResponse)
         else -> error("Received unknown type of QueryResponse: ${queryResponse::class.simpleName}")
      }
   }

   private fun captureQueryResultStreamToHistory(query: Query, queryResult: QueryResult) {
      queryResult.results
         .onEach { typedInstance ->
            queryEventFlow.emit(
               RestfulQueryResultEvent(
                  query, queryResult.queryResponseId, queryResult.clientQueryId, typedInstance
               )
            )
         }
   }

   private suspend fun emitFailure(query: Query, failure: FailedSearchResponse) {
      queryEventFlow.emit(
         RestfulQueryFailureEvent(
            query,
            failure.queryResponseId,
            failure.clientQueryId,
            failure
         )
      )
   }

   suspend fun captureQueryHistory(query: TaxiQlQueryString, queryResponse: QueryResponse) {
      when (queryResponse) {
         is QueryResult -> captureTaxiQlQueryResultStreamToHistory(query, queryResponse)
         is FailedSearchResponse -> emitFailure(query, queryResponse)
         else -> error("Received unknown type of QueryResponse: ${queryResponse::class.simpleName}")
      }
   }

   private fun captureTaxiQlQueryResultStreamToHistory(query: TaxiQlQueryString, queryResult: QueryResult) {
      queryResult.results
         .onEach { typedInstance ->
            queryEventFlow.emit(
               TaxiQlQueryResultEvent(
                  query, queryResult.queryResponseId, queryResult.clientQueryId, typedInstance
               )
            )
         }
   }

   private suspend fun emitFailure(query: TaxiQlQueryString, failure: FailedSearchResponse) {
      queryEventFlow.emit(
         TaxiQlQueryFailureEvent(
            query,
            failure.queryResponseId,
            failure.clientQueryId,
            failure
         )
      )
   }
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
