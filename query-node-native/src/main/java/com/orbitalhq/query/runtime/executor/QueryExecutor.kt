package com.orbitalhq.query.runtime.executor

import com.orbitalhq.query.*
import com.orbitalhq.query.runtime.QueryMessage
import com.orbitalhq.query.runtime.core.QueryLifecycleEventObserver
import com.orbitalhq.query.runtime.executor.analytics.AnalyticsEventWriterProvider
import com.orbitalhq.query.tracing.TraceContext
import com.orbitalhq.query.tracing.TracingEventSink
import com.orbitalhq.schemas.Schema
import com.orbitalhq.utils.Ids
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import mu.KotlinLogging
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(ExperimentalSerializationApi::class)
@Component
class QueryExecutor(
   private val vyneFactory: StandaloneVyneFactory,
   private val eventWriterProvider: AnalyticsEventWriterProvider,
   private val tracingEventSink: TracingEventSink
) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }


//   // TODO : Should be part of the serverless executor.
//   fun executeQuery(messageCborWrapper: QueryMessageCborWrapper): CompressedQueryResultWrapper {
//      return try {
//         val message = messageCborWrapper.message()
//         val result = executeQuery(message)
//         val collectedResult = result.collectList().block()
//         CompressedQueryResultWrapper.forResult(collectedResult!!)
//      } catch (e: Exception) {
//         logger.error(e) { "Query execution failed: ${e.message}" }
//         throw e
//      }
//   }

   /**
    * Executes the query.
    *
    * Currently, there's no support for streaming results,
    * so we can't support stream {} queries, or support streaming
    * incremental results out.
    *
    * As a result, large queries may result in OOM.
    *
    * Also, as queries cannot be streamed, all results are Mono<> (either Mono<List<T>> or Mono<T>)
    */
   fun executeQuery(message: QueryMessage, context: CoroutineContext = EmptyCoroutineContext): Mono<Any> {
      val queryId = Ids.fastUuid()
      val (vyne, discoveryClient) = vyneFactory.buildVyne(message)
      val (eventBroker, eventConsumer) = buildEventBroker(vyne.schema, discoveryClient, queryId)
      val args = message.args()
      return runBlocking {
         val queryResult = vyne.query(
            message.query,
            clientQueryId = message.clientQueryId,
            arguments = args,
            eventBroker = eventBroker,
            queryId = queryId
         )
         val observedQuery = QueryLifecycleEventObserver(eventConsumer, null)
            .responseWithQueryHistoryListener(message.query, queryResult)

         when (observedQuery) {
            is QueryResult -> {
               val resultsFlux = observedQuery.results
                  .asFlux(context)
                  .mapNotNull { it.toRawObject() }

               // As above - we can't support streaming, so everything is either
               // a Mono<List<T>>, or Mono<T>
               if (observedQuery.responseType.isCollection) {
                  resultsFlux.collectList()
               } else {
                  resultsFlux.single()
               }
            }

            is FailedQueryResponse -> Mono.error<Any>(QueryFailedException(observedQuery.message))
            else -> error("Received unknown type of QueryResponse: ${observedQuery::class.simpleName}")
         } as Mono<Any>
      }

   }

   private fun buildEventBroker(
      schema: Schema,
      discoveryClient: DiscoveryClient,
      queryId: String,
   ): Pair<QueryContextEventBroker, QueryEventConsumer> {
      val eventBroker = QueryContextEventBroker(traceSpan = TraceContext.newTrace(queryId, tracingEventSink).rootSpan)
      val eventConsumer =
         eventWriterProvider.buildAnalyticsEventConsumer(queryId, schema, discoveryClient)
      eventBroker.addHandler(eventConsumer)

      return eventBroker to eventConsumer
   }

}
