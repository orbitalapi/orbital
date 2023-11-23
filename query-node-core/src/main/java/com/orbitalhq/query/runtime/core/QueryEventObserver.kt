package com.orbitalhq.query.runtime.core

import com.orbitalhq.query.FailedQueryResponse
import com.orbitalhq.query.Query
import com.orbitalhq.query.QueryCancelledException
import com.orbitalhq.query.QueryCompletedEvent
import com.orbitalhq.query.QueryEventConsumer
import com.orbitalhq.query.QueryFailureEvent
import com.orbitalhq.query.QueryResponse
import com.orbitalhq.query.QueryResult
import com.orbitalhq.query.QueryStartEvent
import com.orbitalhq.query.RestfulQueryExceptionEvent
import com.orbitalhq.query.RestfulQueryResultEvent
import com.orbitalhq.query.StreamingQueryCancelledEvent
import com.orbitalhq.query.TaxiQlQueryExceptionEvent
import com.orbitalhq.query.TaxiQlQueryResultEvent
import com.orbitalhq.query.runtime.core.monitor.ActiveQueryMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import lang.taxi.query.TaxiQLQueryString
import mu.KotlinLogging
import java.time.Instant
import java.util.concurrent.Executors

private val statsDispatcher = Executors.newFixedThreadPool(16).asCoroutineDispatcher()

/**
 * Observes lifecycle events from the query, and creates QueryEventConsumer events.
 *
 * Specifically, the following events occur:
 *  - QueryStarted
 *  - QueryResult
 *  - QueryEnded
 *
 *  Don't love this design, and suspect we can simplify this if we try hard.
 */
class QueryLifecycleEventObserver(
   private val consumer: QueryEventConsumer,
   private val activeQueryMonitor: ActiveQueryMonitor?,
) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   private val statisticsScope = CoroutineScope(statsDispatcher)

   /**
    * Attaches an observer to the result flow of the QueryResponse, returning
    * an updated QueryResponse with it's internal flow updated.
    *
    * Callers should only used the returned value.
    */
   fun responseWithQueryHistoryListener(query: Query, queryResponse: QueryResponse): QueryResponse {
      return when (queryResponse) {
         is QueryResult -> captureQueryResultStreamToHistory(query, queryResponse)
         is FailedQueryResponse -> emitFailure(queryResponse)
         else -> error("Received unknown type of QueryResponse: ${queryResponse::class.simpleName}")
      }
   }

   private fun captureQueryResultStreamToHistory(query: Query, queryResult: QueryResult): QueryResult {
      val queryStartTime = Instant.now()
      consumer.handleEvent(
         QueryStartEvent(
            taxiQuery = null,
            query = query,
            message = queryResult.responseType ?: "",
            queryId = queryResult.queryId,
            clientQueryId = queryResult.clientQueryId ?: queryResult.queryId,
            timestamp = queryStartTime
         )
      )

      return queryResult.copy(
         results = queryResult.results
            .onEach { typedInstance ->

               activeQueryMonitor?.incrementEmittedRecordCount(queryId = queryResult.queryResponseId)
               val event = RestfulQueryResultEvent(
                  query, queryResult.queryResponseId, queryResult.clientQueryId, typedInstance, queryStartTime
               )
               consumer.handleEvent(event)
//               metricsEventConsumer.handleEvent(event)

            }
            .onCompletion { error ->
               if (error == null) {
                  val event = QueryCompletedEvent(
                     queryId = queryResult.queryResponseId,
                     timestamp = Instant.now(),
                     clientQueryId = queryResult.clientQueryId,
                     message = "",
                     query = query.toString(),
                     recordCount = activeQueryMonitor?.queryMetaData(queryResult.queryResponseId)?.completedProjections
                        ?: 0
                  )

                  consumer.handleEvent(event)
//                  metricsEventConsumer.handleEvent(event)
               } else {
                  val event = RestfulQueryExceptionEvent(
                     query,
                     queryResult.queryResponseId,
                     queryResult.clientQueryId,
                     Instant.now(),
                     error.message ?: "No message provided",
                     queryStartTime
                  )
                  consumer.handleEvent(event)
//                  metricsEventConsumer.handleEvent(event)
               }
               activeQueryMonitor?.reportComplete(queryResult.queryId)
            }.catch {
               logger.warn { "An error in emitting results - has consumer gone away?? ${it.message} ${it.javaClass}" }
               activeQueryMonitor?.reportComplete(queryResult.queryId)
               throw it
            }
      )
   }

   /**
    * Attaches an observer to the result flow of the QueryResponse, returning
    * an updated QueryResponse with it's internal flow updated.
    *
    * Callers should only used the returned value.
    */
   fun responseWithQueryHistoryListener(query: TaxiQLQueryString, queryResponse: QueryResponse): QueryResponse {
      return when (queryResponse) {
         is QueryResult -> captureTaxiQlQueryResultStreamToHistory(query, queryResponse)
         is FailedQueryResponse -> emitFailure(queryResponse)
         else -> error("Received unknown type of QueryResponse: ${queryResponse::class.simpleName}")
      }
   }

   private fun captureTaxiQlQueryResultStreamToHistory(
      query: TaxiQLQueryString,
      queryResult: QueryResult
   ): QueryResult {
      val queryStartTime = Instant.now()
      consumer.handleEvent(
         QueryStartEvent(
            taxiQuery = query,
            query = null,
            message = queryResult.responseType ?: "",
            queryId = queryResult.queryId,
            clientQueryId = queryResult.clientQueryId ?: queryResult.queryId,
            timestamp = queryStartTime
         )
      )

      return queryResult.copy(
         results = queryResult.results
            .onEach { typedInstance ->
               activeQueryMonitor?.incrementEmittedRecordCount(queryId = queryResult.queryResponseId)
               val event = TaxiQlQueryResultEvent(
                  query,
                  queryResult.queryResponseId,
                  queryResult.clientQueryId,
                  typedInstance,
                  queryResult.anonymousTypes,
                  queryStartTime
               )
               consumer.handleEvent(event)
//               metricsEventConsumer.handleEvent(event)
            }
            .onCompletion { error ->
               if (error == null) {

                  val event = QueryCompletedEvent(
                     queryId = queryResult.queryResponseId,
                     timestamp = Instant.now(),
                     clientQueryId = queryResult.clientQueryId,
                     message = "",
                     query = query,
                     recordCount = activeQueryMonitor?.queryMetaData(queryResult.queryResponseId)?.completedProjections
                        ?: 0
                  )

                  consumer.handleEvent(event)
//                  metricsEventConsumer.handleEvent(event)
               } else {
                  val event = when (error) {
                     is QueryCancelledException -> StreamingQueryCancelledEvent(
                        query,
                        queryResult.queryResponseId,
                        queryResult.clientQueryId,
                        Instant.now(),
                        error.message ?: "No message provided",
                        queryStartTime,
                        activeQueryMonitor?.queryMetaData(queryResult.queryResponseId)?.completedProjections ?: 0
                     )

                     else -> TaxiQlQueryExceptionEvent(
                        query,
                        queryResult.queryResponseId,
                        queryResult.clientQueryId,
                        Instant.now(),
                        error.message ?: "No message provided",
                        queryStartTime,
                        activeQueryMonitor?.queryMetaData(queryResult.queryResponseId)?.completedProjections ?: 0
                     )
                  }
                  consumer.handleEvent(event)
//                  metricsEventConsumer.handleEvent(event)
               }
               activeQueryMonitor?.reportComplete(queryResult.queryId)
//               statsCollector.cancel()
            }.catch {
               logger.warn { "An error in emitting results - has consumer gone away?? ${it.message}" }
               activeQueryMonitor?.reportComplete(queryResult.queryId)
//               statsCollector.cancel()
               throw it
            }
      )

   }

   private fun emitFailure(failure: FailedQueryResponse): FailedQueryResponse {
      val event = QueryFailureEvent(
         failure.queryResponseId,
         failure.clientQueryId,
         failure
      )
      consumer.handleEvent(event)
//      metricsEventConsumer.handleEvent(event)
      return failure
   }
}
