package io.vyne.query.runtime.core.gateway

import io.vyne.query.runtime.FailedSearchResponse
import io.vyne.schema.api.SchemaSet
import io.vyne.schema.consumer.SchemaStore
import lang.taxi.query.TaxiQlQuery
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.status
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toFlux

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
   private val executor: RoutedQueryExecutor?
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
         val query = queryRouter.getQuery(request)
         if (request.path().startsWith("/api/q") && query == null) {
            logger.warn { "Request $request received did not match any queries" }
         }
         if (query != null) {
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
      return Mono.just(query)
         .subscribeOn(Schedulers.boundedElastic())
         // TODO : Handle streaming queries.
         // For now, everything is a Mono<>
         .flatMap {
            executor.handleRoutedQuery(query)
               .single()
               .flatMap { response: Any ->
                  logger.info { "Received response : $response" }
                  ServerResponse.ok().body<Any>(Mono.just(response))
               }
         }
         .onErrorResume { e ->
            logger.warn { "Query failed with error ${e.message}" }
            ServerResponse.status(HttpStatus.BAD_REQUEST)
               .bodyValue(
                  FailedSearchResponse(e.message ?: "Query failed with exception ${e::class.simpleName}", queryId = "")
               )

         }
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

      }
   }
}

