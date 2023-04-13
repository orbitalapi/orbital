package io.vyne.queryProxy.router

import io.vyne.schema.api.SchemaSet
import io.vyne.schema.consumer.SchemaStore
import io.vyne.spring.VyneFactory
import lang.taxi.query.TaxiQlQuery
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.status
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

@Component
class QueryRequestHandler(
   private val schemaStore: SchemaStore,
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
      logger.info { "Router updated, now contains the following routes: \n ${queryRouter.routes.joinToString("\n")}" }
   }

   fun handleQuery(request: ServerRequest, query: RoutedQuery): Mono<ServerResponse> {
      TODO()
   }

   override fun handle(request: ServerRequest): Mono<ServerResponse> {
      val query = request.attributes()[MATCHED_QUERY] as? TaxiQlQuery
      return if (query == null) {
         logger.warn { "Request $request did not match a query, which is unexpected - did the schema just change?" }
         status(HttpStatus.NOT_FOUND).build()
      } else {
         val routedQuery = RoutedQuery.build(query, request)
         handleQuery(request, routedQuery)
      }
   }
}

