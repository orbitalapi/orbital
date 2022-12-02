package io.vyne.history

import io.vyne.query.FailedQueryResponse
import io.vyne.query.Query
import io.vyne.query.QueryCancelledException
import io.vyne.query.QueryCompletedEvent
import io.vyne.query.QueryEventConsumer
import io.vyne.query.QueryFailureEvent
import io.vyne.query.QueryResponse
import io.vyne.query.QueryResult
import io.vyne.query.QueryStartEvent
import io.vyne.query.RestfulQueryExceptionEvent
import io.vyne.query.RestfulQueryResultEvent
import io.vyne.query.StreamingQueryCancelledEvent
import io.vyne.query.TaxiQlQueryExceptionEvent
import io.vyne.query.TaxiQlQueryResultEvent
import io.vyne.query.VyneQueryStatisticsEvent
import io.vyne.query.active.ActiveQueryMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import lang.taxi.query.TaxiQLQueryString
import mu.KotlinLogging
import java.time.Instant
import java.util.concurrent.Executors

private val logger = KotlinLogging.logger {}

private val statsDispatcher = Executors.newFixedThreadPool(16).asCoroutineDispatcher()

class QueryEventObserver(
   private val consumer: QueryEventConsumer,
   private val activeQueryMonitor: ActiveQueryMonitor,
   private val metricsEventConsumer: QueryEventConsumer) {

   private val statisticsScope = CoroutineScope(statsDispatcher)

   /**
    * Attaches an observer to the result flow of the QueryResponse, returning
    * an updated QueryResponse with it's internal flow updated.
    *
    * Callers should only used the returned value.
    */
   suspend fun responseWithQueryHistoryListener(query: Query, queryResponse: QueryResponse): QueryResponse {
      return when (queryResponse) {
         is QueryResult -> captureQueryResultStreamToHistory(query, queryResponse)
         is FailedQueryResponse -> emitFailure(query, queryResponse)
         else -> error("Received unknown type of QueryResponse: ${queryResponse::class.simpleName}")
      }
   }

   private fun captureQueryResultStreamToHistory(query: Query, queryResult: QueryResult): QueryResult {
      val queryStartTime = Instant.now()
      consumer.handleEvent(QueryStartEvent(
         taxiQuery = null,
         query = query,
         message = queryResult.responseType ?: "",
         queryId = queryResult.queryId,
         clientQueryId = queryResult.clientQueryId ?: queryResult.queryId,
         timestamp = queryStartTime
      ))
      val statsCollector = statisticsScope.launch {
         queryResult.statistics?.collect {

         }
      }

      return queryResult.copy(
         results = queryResult.results
            .onEach { typedInstance ->

               activeQueryMonitor.incrementEmittedRecordCount(queryId = queryResult.queryResponseId)
               val event = RestfulQueryResultEvent(
                  query, queryResult.queryResponseId, queryResult.clientQueryId, typedInstance, queryStartTime
               )
               consumer.handleEvent(event)
               metricsEventConsumer.handleEvent(event)

            }
            .onCompletion { error ->
               if (error == null) {
                  val event = QueryCompletedEvent(
                     queryId = queryResult.queryResponseId,
                     timestamp = Instant.now(),
                     clientQueryId = queryResult.clientQueryId,
                     message = "",
                     query = query.toString(),
                     recordCount = activeQueryMonitor.queryMetaData(queryResult.queryResponseId)?.completedProjections ?: 0
                  )

                  consumer.handleEvent(event)
                  metricsEventConsumer.handleEvent(event)
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
                  metricsEventConsumer.handleEvent(event)
               }
               activeQueryMonitor.reportComplete(queryResult.queryId)
               statsCollector.cancel()
            }.catch {
               logger.warn { "An error in emitting results - has consumer gone away?? ${it.message} ${it.javaClass}" }
               activeQueryMonitor.reportComplete(queryResult.queryId)
               statsCollector.cancel()
               throw it
            }
      )
   }

   private suspend fun emitFailure(query: Query, failure: FailedQueryResponse): FailedQueryResponse {

      val event = QueryFailureEvent(
         failure.queryResponseId,
         failure.clientQueryId,
         failure
      )
      consumer.handleEvent(event)
      metricsEventConsumer.handleEvent(event)
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
         is FailedQueryResponse -> emitFailure(query, queryResponse)
         else -> error("Received unknown type of QueryResponse: ${queryResponse::class.simpleName}")
      }
   }

   private fun captureTaxiQlQueryResultStreamToHistory(
      query: TaxiQLQueryString,
      queryResult: QueryResult
   ): QueryResult {
      val queryStartTime = Instant.now()
      consumer.handleEvent(QueryStartEvent(
         taxiQuery = query,
         query = null,
         message = queryResult.responseType ?: "",
         queryId = queryResult.queryId,
         clientQueryId = queryResult.clientQueryId ?: queryResult.queryId,
         timestamp = queryStartTime)
      )

      val statsCollector = statisticsScope.launch {
         queryResult.statistics?.collect {
            metricsEventConsumer.handleEvent(VyneQueryStatisticsEvent(it))
         }
      }

      return queryResult.copy(
         results = queryResult.results

            .onEach { typedInstance ->
               activeQueryMonitor.incrementEmittedRecordCount(queryId = queryResult.queryResponseId)
               val event = TaxiQlQueryResultEvent(
                  query,
                  queryResult.queryResponseId,
                  queryResult.clientQueryId,
                  typedInstance,
                  queryResult.anonymousTypes,
                  queryStartTime
               )
               consumer.handleEvent(event)
               metricsEventConsumer.handleEvent(event)
            }
            .onCompletion { error ->
               if (error == null) {

                  val event = QueryCompletedEvent(
                     queryId = queryResult.queryResponseId,
                     timestamp = Instant.now(),
                     clientQueryId = queryResult.clientQueryId,
                     message = "",
                     query = query,
                     recordCount = activeQueryMonitor.queryMetaData(queryResult.queryResponseId)?.completedProjections ?: 0
                  )

                  consumer.handleEvent(event)
                  metricsEventConsumer.handleEvent(event)
               } else {
                  val event = when (error) {
                     is QueryCancelledException -> StreamingQueryCancelledEvent(
                        query,
                        queryResult.queryResponseId,
                        queryResult.clientQueryId,
                        Instant.now(),
                        error.message ?: "No message provided",
                        queryStartTime,
                        activeQueryMonitor.queryMetaData(queryResult.queryResponseId)?.completedProjections ?: 0
                     )
                     else -> TaxiQlQueryExceptionEvent(
                        query,
                        queryResult.queryResponseId,
                        queryResult.clientQueryId,
                        Instant.now(),
                        error.message ?: "No message provided",
                        queryStartTime,
                        activeQueryMonitor.queryMetaData(queryResult.queryResponseId)?.completedProjections ?: 0
                     )
                  }
                  consumer.handleEvent(event)
                  metricsEventConsumer.handleEvent(event)
               }
               activeQueryMonitor.reportComplete(queryResult.queryId)
               statsCollector.cancel()
            }.catch {
               logger.warn { "An error in emitting results - has consumer gone away?? ${it.message}" }
               activeQueryMonitor.reportComplete(queryResult.queryId)
               statsCollector.cancel()
               throw it
            }
      )

   }

   private suspend fun emitFailure(query: TaxiQLQueryString, failure: FailedQueryResponse): FailedQueryResponse {
      val event = QueryFailureEvent(
         failure.queryResponseId,
         failure.clientQueryId,
         failure
      )
      consumer.handleEvent(event)
      metricsEventConsumer.handleEvent(event)
      return failure
   }
}

