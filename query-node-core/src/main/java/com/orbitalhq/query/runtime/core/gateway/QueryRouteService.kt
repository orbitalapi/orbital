package com.orbitalhq.query.runtime.core.gateway

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Throwables
import com.orbitalhq.auth.EmptyAuthenticationToken
import com.orbitalhq.metrics.QueryMetricsReporter
import com.orbitalhq.query.HistoryEventConsumerProvider
import com.orbitalhq.query.MetricTags
import com.orbitalhq.query.tagsOf
import com.orbitalhq.query.tracing.HttpRequest
import com.orbitalhq.query.tracing.HttpResponse
import com.orbitalhq.query.tracing.NoopTracingEventSink
import com.orbitalhq.query.tracing.SpanEventSource
import com.orbitalhq.query.tracing.SpanState
import com.orbitalhq.query.tracing.TraceEventDirection
import com.orbitalhq.query.tracing.TracingEvent
import com.orbitalhq.query.tracing.TracingEventKind
import com.orbitalhq.schema.api.SchemaSet
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.spring.http.HttpStatusException
import com.orbitalhq.spring.invokers.asVyneHeadersMap
import lang.taxi.query.QueryMode
import lang.taxi.query.TaxiQlQuery
import mu.KotlinLogging
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.status
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import java.security.Principal
import java.time.Instant
import kotlin.jvm.Throws

/**
 * The handler / service which receives HTTP invocations
 * of queries annotated with @Http.
 *
 * Hands off execution to a RoutedQueryExecutor - meaning queries
 * are either executed in-process, or routed to a serverless function somewhere.
 *
 */
@Component
class QueryRouteService(
   private val schemaStore: SchemaStore,
   private val executor: RoutedQueryExecutor?,
   private val queryPrefix: String = "/api/q/",
   private val metricsReporter: QueryMetricsReporter,
   private val objectMapper: ObjectMapper,
   private val eventConsumerProvider: HistoryEventConsumerProvider
) : HandlerFunction<ServerResponse> {

   private var queryRouter: QueryRouter = QueryRouter.build(emptyList())

   companion object {
      private val logger = KotlinLogging.logger {}
      private val MATCHED_QUERY = "MATCHED_QUERY"
   }


   init {
      schemaStore.schemaChanged
         .toFlux()
         .subscribe { event -> buildQueryIndex(event.newSchemaSet) }
      buildQueryIndex(schemaStore.schemaSet)
   }

   val routes: List<RoutableQuery>
      get() {
         return queryRouter.routes
      }

   // For testing
   fun findRoute(path: String, method: HttpMethod): TaxiQlQuery? {
      return queryRouter.getQuery(path, method)
   }

   fun router(): RouterFunction<ServerResponse> {
      return RouterFunctions.route({ request ->
         // Short circut
         if (!request.path().startsWith(queryPrefix)) {
            return@route false
         }
         val query = queryRouter.getQuery(request)
         if (query == null) {
            logger.warn { "Request $request received did not match any queries" }
         } else {
            logger.debug { "Request $request mapped to query ${query.name}" }
            request.attributes()[MATCHED_QUERY] = query
         }
         query != null
      }, this)
   }

   private fun buildQueryIndex(schemaSet: SchemaSet) {
      logger.info { "Schema changed, rebuilding query handlers for schema generation ${schemaSet.generation}" }
      queryRouter = QueryRouter.build(schemaSet.schema.taxi.queries)
      logger.info { "Router updated, now contains the following routes: \n${queryRouter.routes.joinToString("\n")}" }
   }

   fun handleQuery(request: ServerRequest, query: RoutedQuery): Mono<ServerResponse> {
      if (executor == null) {
         logger.warn { "Received query invocation on ${request.path()} - matches with query ${query.query.name} - but no query executor exists.  Returning a not found error" }
         return ServerResponse.notFound().build()
      }
      logger.info { "Received query invocation on ${request.path()} - matches with query ${query.query.name}" }
      val logDurationsOfIndividualMessages = query.query.queryMode == QueryMode.STREAM
      val principal = request.principal() as Mono<Principal>
      return principal
         .defaultIfEmpty(EmptyAuthenticationToken)
         .flatMap { principal ->
            val queryResultPublisher =
               executor.handleRoutedQuery(query, EmptyAuthenticationToken.nullIfEmpty(principal))
                  .let {
                     metricsReporter.observeQueryResult(
                        it.publisher,
                        Instant.now(),
                        getMetricsTags(query),
                        logDurationsOfIndividualMessages
                     ) to it.responseHeaders
                  }

            val responseStream = queryResultPublisher.first
            val responseHeaders = queryResultPublisher.second
            val returnServerSentEvents = request.headers().accept().contains(MediaType.TEXT_EVENT_STREAM)
            if (returnServerSentEvents && responseStream is Flux<*>) {
               DeferredServerResponsePublisher.wrapEventStreamFlux(responseStream, responseHeaders)
            } else if (responseStream is Flux<*>) {
               DeferredServerResponsePublisher.wrapFlux(responseStream as Flux<out Any>, responseHeaders)
            } else if (responseStream is Mono<*>) {
               DeferredServerResponsePublisher.wrapMono(responseStream as Mono<Any>, responseHeaders)
            } else {
               error("Unexpected type of publisher: ${responseStream::class.simpleName}")
            }
         }
   }

   private fun getMetricsTags(query: RoutedQuery): MetricTags {
      return tagsOf().queryStream(query.query.name.fullyQualifiedName)
         .tags()
   }

   override fun handle(request: ServerRequest): Mono<ServerResponse> {
      val query = request.attributes()[MATCHED_QUERY] as? TaxiQlQuery
      val spanId = TracingEvent.newSpanId()
      return if (query == null) {
         logger.warn { "Request $request did not match a query, which is unexpected - did the schema just change?" }
         status(HttpStatus.NOT_FOUND).build()
      } else {
         val querySource = query.compilationUnits.single().source.content
         RoutedQuery.build(query, querySource, request)
            .flatMap { routedQuery ->
               val queryOptions = QueryOptions.fromQuery(query)
               val eventSink = try {
                  eventConsumerProvider.createTraceEventSink(
                     routedQuery.clientQueryId, routedQuery.rootTraceId, schemaStore.schema(),
                     queryOptions
                  )
               } catch (e: Throwable) {
                  val rootCause = Throwables.getRootCause(e)
                  logger.warn(rootCause) { "Failed to create a trace event sink - events will be dropped for this phase of the query " }
                  NoopTracingEventSink
               }

               request.bodyToMono<String>()
                  .doOnNext {

                  }
               eventSink.emitEvent(
                  QueryRouterSpanEventSource(queryOptions, routedQuery), TracingEvent(
                     routedQuery.clientQueryId,
                     routedQuery.rootTraceId,
                     spanId,
                     routedQuery.rootTraceId,
                     TracingEventKind.OK,
                     TraceEventDirection.INBOUND,
                     SpanState.ACTIVE,
                     HttpRequest(
                        request.uri().toASCIIString(),
                        request.method().name(),
                        { "Body not captured yet"},
                        0,
                        request.headers().asHttpHeaders().asVyneHeadersMap()
                     ),
                     request.method().name(),
                     "Router",
                     "Router",
                     null
                  )
               )

               handleQuery(request, routedQuery)
                  .doOnNext { response ->
                     val eventKind = if (response.statusCode().isError) {
                        TracingEventKind.ERROR
                     } else {
                        TracingEventKind.OK
                     }
                     eventSink.emitEvent(
                        QueryRouterSpanEventSource(queryOptions, routedQuery), TracingEvent(
                           routedQuery.clientQueryId,
                           routedQuery.rootTraceId,
                           spanId,
                           routedQuery.rootTraceId,
                           eventKind,
                           TraceEventDirection.OUTBOUND,
                           SpanState.COMPLETE,
                           HttpResponse(
                              response.statusCode().value(),
                              { "Responses not captured" },
                              -1,
                              response.headers().asVyneHeadersMap()
                           ),
                           request.method().name(),
                           "Router",
                           "Router",
                           null
                        )
                     )
                  }
                  .doOnError { error ->
                     eventSink.emitEvent(
                        QueryRouterSpanEventSource(queryOptions, routedQuery), TracingEvent(
                           routedQuery.clientQueryId,
                           routedQuery.rootTraceId,
                           spanId,
                           routedQuery.rootTraceId,
                           TracingEventKind.ERROR,
                           TraceEventDirection.OUTBOUND,
                           SpanState.COMPLETE,
                           HttpResponse(
                              -1,
                              { Throwables.getRootCause(error).message },
                              -1,
                              emptyMap()
                           ),
                           request.method().name(),
                           "Router",
                           "Router",
                           null
                        )
                     )
                  }
            }
            .onErrorResume { e ->
               when (e) {
                  is HttpStatusException -> status(e.status.value()).bodyValue(e.message)
                  else -> status(HttpStatus.INTERNAL_SERVER_ERROR).bodyValue(e.message)
               }
            }

      }
   }
}


data class QueryRouterSpanEventSource(
   val queryOptions: QueryOptions,
   val routedQuery: RoutedQuery
) : SpanEventSource
