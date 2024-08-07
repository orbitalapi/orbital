package com.orbitalhq.query.runtime.core.gateway

import com.orbitalhq.metrics.QueryMetricsReporter
import com.orbitalhq.query.MetricTags
import com.orbitalhq.auth.EmptyAuthenticationToken
import com.orbitalhq.query.tagsOf
import com.orbitalhq.schema.api.SchemaSet
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.spring.http.HttpStatusException
import lang.taxi.query.QueryMode
import lang.taxi.query.TaxiQlQuery
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.status
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import java.security.Principal
import java.time.Instant

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
            val queryResultPublisher = executor.handleRoutedQuery(query, EmptyAuthenticationToken.nullIfEmpty(principal))
               .let {
                  metricsReporter.observeQueryResult(
                     it,
                     Instant.now(),
                     getMetricsTags(query),
                     logDurationsOfIndividualMessages
                  )
               }

            val returnServerSentEvents = request.headers().accept().contains(MediaType.TEXT_EVENT_STREAM)
            if (returnServerSentEvents && queryResultPublisher is Flux<*>) {
               DeferredServerResponsePublisher.wrapEventStreamFlux(queryResultPublisher)
                  } else if (queryResultPublisher is Flux<*>){
               DeferredServerResponsePublisher.wrapFlux(queryResultPublisher as Flux<out Any>)
            } else if (queryResultPublisher is Mono<*>){
               DeferredServerResponsePublisher.wrapMono(queryResultPublisher as Mono<Any>)
            } else {
               error("Unexpected type of publisher: ${queryResultPublisher::class.simpleName}")
            }
         }
   }

   private fun getMetricsTags(query: RoutedQuery): MetricTags {
      return tagsOf().queryStream(query.query.name.fullyQualifiedName)
         .tags()
   }

   override fun handle(request: ServerRequest): Mono<ServerResponse> {
      val query = request.attributes()[MATCHED_QUERY] as? TaxiQlQuery
      return if (query == null) {
         logger.warn { "Request $request did not match a query, which is unexpected - did the schema just change?" }
         status(HttpStatus.NOT_FOUND).build()
      } else {
         val querySource = query.compilationUnits.single().source.content
         RoutedQuery.build(query, querySource, request)
            .flatMap { routedQuery ->
               handleQuery(request, routedQuery)
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

