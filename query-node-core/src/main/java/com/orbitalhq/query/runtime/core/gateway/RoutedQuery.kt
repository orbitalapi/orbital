package com.orbitalhq.query.runtime.core.gateway

import lang.taxi.annotations.HttpPathVariable
import lang.taxi.annotations.HttpRequestBody
import lang.taxi.query.FactValue
import lang.taxi.query.Parameter
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery
import lang.taxi.types.TypedValue
import lang.taxi.types.annotation
import org.springframework.web.reactive.function.server.ServerRequest
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * A query that has matched a route.
 * Contains the query itself, and the parameter facts
 * extracted from the inbound request
 */
data class RoutedQuery(
   val query: TaxiQlQuery,
   val querySrc: TaxiQLQueryString,
   val arguments: Map<Parameter, FactValue>
) {

   val argumentValues: Map<String, Any?> = arguments.map { (param, value) ->
      param.name to value.typedValue.value
   }.toMap()

   companion object {
      fun build(query: TaxiQlQuery, querySrc: TaxiQLQueryString, request: ServerRequest): Mono<RoutedQuery> {
         return Flux.fromIterable(query.parameters)
            .flatMap { parameter ->
               extractParameterValueFromRequest(parameter, request)
                  .map { parameter to it }
            }
            .collectList()
            .map { v ->
               RoutedQuery(query, querySrc, v.toMap())
            }
      }


      private fun extractParameterValueFromRequest(parameter: Parameter, request: ServerRequest): Mono<FactValue> {
         return when {
            pathVariableName(parameter) != null -> Mono.just(request.pathVariable(pathVariableName(parameter)!!))
            isRequestBody(parameter) -> request.bodyToMono(String::class.java)
            // TODO : Others, lke query string, etc

            // TODO : This should result in a BadRequest, somehow...
            else -> error("Parameter ${parameter.name} was not provided through the request")
         }
            .map { rawValue -> FactValue.Constant(TypedValue(parameter.type, rawValue)) }
      }

      private fun isRequestBody(parameter: Parameter): Boolean {
         return parameter.annotation(HttpRequestBody.NAME) != null
      }

      private fun pathVariableName(parameter: Parameter): String? {
         return parameter.annotation(HttpPathVariable.NAME)?.let { it.defaultParameterValue?.toString() }
      }
   }
}
