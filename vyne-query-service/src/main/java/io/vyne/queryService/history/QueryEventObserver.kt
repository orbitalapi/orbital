package io.vyne.queryService.history

import io.vyne.models.TypedInstance
import io.vyne.query.Query
import io.vyne.query.QueryResponse
import io.vyne.query.QueryResult
import io.vyne.query.RemoteCallOperationResultHandler
import io.vyne.query.active.ActiveQueryMonitor
import io.vyne.queryService.query.FailedSearchResponse
import io.vyne.queryService.query.MetricsEventConsumer
import io.vyne.schemas.Type
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import lang.taxi.types.TaxiQLQueryString
import mu.KotlinLogging
import org.reflections8.Reflections.collect
import java.time.Instant
import java.util.concurrent.Executors

/**
 * Takes a queries results, metadata, etc, and streams the out to a QueryHistory provider
 * to be captured.
 */
private val logger = KotlinLogging.logger {}

private val statisticsScope = CoroutineScope(Executors.newFixedThreadPool(16).asCoroutineDispatcher())

class QueryEventObserver(private val consumer: QueryEventConsumer, private val activeQueryMonitor: ActiveQueryMonitor, private val metricsEventConsumer: MetricsEventConsumer) {
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
      val queryStartTime = Instant.now()

      val statsCollector = statisticsScope.launch {
         launch { queryResult.statistics?.collect {

            metricsEventConsumer.counterGraphFailedSearch.increment(it.graphSearchFailedCount.toDouble())
            metricsEventConsumer.counterGraphSearch.increment(it.graphSearchSuccessCount.toDouble())
            metricsEventConsumer.counterGraphBuild.increment(it.graphCreatedCount.toDouble())

             }
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
                     queryResult.queryResponseId,
                     Instant.now()
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
               logger.warn { "An error in emitting results - has consumer gone away?? ${it.message}" }
               activeQueryMonitor.reportComplete(queryResult.queryId)
               statsCollector.cancel()
            }
      )
   }

   private suspend fun emitFailure(query: Query, failure: FailedSearchResponse): FailedSearchResponse {

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
         is FailedSearchResponse -> emitFailure(query, queryResponse)
         else -> error("Received unknown type of QueryResponse: ${queryResponse::class.simpleName}")
      }
   }

   private fun captureTaxiQlQueryResultStreamToHistory(
      query: TaxiQLQueryString,
      queryResult: QueryResult
   ): QueryResult {
      val queryStartTime = Instant.now()

      val statsCollector = statisticsScope.launch {
         launch { queryResult.statistics?.collect {

            metricsEventConsumer.counterGraphFailedSearch.increment(it.graphSearchFailedCount.toDouble())
            metricsEventConsumer.counterGraphSearch.increment(it.graphSearchSuccessCount.toDouble())
            metricsEventConsumer.counterGraphBuild.increment(it.graphCreatedCount.toDouble())
         }

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
                     queryResult.queryResponseId,
                     Instant.now()
                  )
                  consumer.handleEvent(event)
                  metricsEventConsumer.handleEvent(event)
               } else {
                  val event = TaxiQlQueryExceptionEvent(
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
               logger.warn { "An error in emitting results - has consumer gone away?? ${it.message}" }
               activeQueryMonitor.reportComplete(queryResult.queryId)
               statsCollector.cancel()
            }
      )

   }

   private suspend fun emitFailure(query: TaxiQLQueryString, failure: FailedSearchResponse): FailedSearchResponse {
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

interface QueryEventConsumer : RemoteCallOperationResultHandler {
   fun handleEvent(event: QueryEvent)
}

sealed class QueryEvent

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
   val failure: FailedSearchResponse
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
   val timestamp: Instant
) : QueryEvent()

data class TaxiQlQueryExceptionEvent(
   val query: TaxiQLQueryString,
   val queryId: String,
   val clientQueryId: String?,
   val timestamp: Instant,
   val message: String,
   val queryStartTime: Instant
) : QueryEvent()

data class RestfulQueryExceptionEvent(
   val query: Query,
   val queryId: String,
   val clientQueryId: String?,
   val timestamp: Instant,
   val message: String,
   val queryStartTime: Instant
) : QueryEvent()
