package io.vyne.query.runtime.core.gateway

import lang.taxi.annotations.HttpOperation
import lang.taxi.query.TaxiQlQuery
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.server.RequestPredicate
import org.springframework.web.reactive.function.server.RequestPredicates
import org.springframework.web.reactive.function.server.ServerRequest

/**
 * Matches inbound ServerRequest with a corresponding
 * TaxiQL query (annotated with @HttpOperation)
 */
class QueryRouter private constructor(val routes: List<RoutableQuery>) {

   fun getQuery(request: ServerRequest): TaxiQlQuery? {
      return routes.firstOrNull { routableQuery ->
         routableQuery.predicate.test(request)
      }?.let { routableQuery -> routableQuery.query }
   }


   companion object {
      /**
       * Takes a series of TaxiQL queries, and builds a QueryRouter
       * that contains a RoutableQuery for each query decorated with
       * an @HttpOperation annotation.
       */
      fun build(queries: Iterable<TaxiQlQuery>): QueryRouter {

         val routes = queries.mapNotNull { query ->
            val httpAnnotation = query.annotations.singleOrNull { annotation -> annotation.name == HttpOperation.NAME }
            if (httpAnnotation != null) {
               query to HttpOperation.fromAnnotation(httpAnnotation)
            } else null
         }.map { (query, httpOperation) ->
            val predicate = when (val method = getHttpMethod(httpOperation.method)) {
               HttpMethod.GET -> RequestPredicates.GET(httpOperation.url)
               HttpMethod.POST -> RequestPredicates.POST(httpOperation.url)
               HttpMethod.PUT -> RequestPredicates.PUT(httpOperation.url)
               HttpMethod.DELETE -> RequestPredicates.DELETE(httpOperation.url)
               else -> error("HttpMethod $method is not supported")
            }
            RoutableQuery(query, predicate, httpOperation)
         }
         return QueryRouter(routes)
      }

      private fun getHttpMethod(method: String): HttpMethod = HttpMethod.valueOf(method)
   }


}


/**
 * A query, with an associated HTTP route
 * (defined as a spring RequestPredicate)
 */
data class RoutableQuery(
   val query: TaxiQlQuery,
   val predicate: RequestPredicate,
   val annotation: HttpOperation
) {
   override fun toString(): String = "${annotation.method} ${annotation.url} -> ${query.name}"
}

