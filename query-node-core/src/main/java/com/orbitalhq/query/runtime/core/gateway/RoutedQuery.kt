package com.orbitalhq.query.runtime.core.gateway

import com.orbitalhq.models.TypedNull
import com.orbitalhq.spring.http.HttpStatusException
import lang.taxi.annotations.HttpHeader
import lang.taxi.annotations.HttpPathVariable
import lang.taxi.annotations.HttpQueryVariable
import lang.taxi.annotations.HttpRequestBody
import lang.taxi.annotations.HttpResponseHeader
import lang.taxi.query.FactValue
import lang.taxi.query.Parameter
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery
import lang.taxi.types.TypedValue
import lang.taxi.types.annotation
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.util.*
import kotlin.jvm.optionals.getOrNull

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
         // This is a hack.
         // See below...

         return when {
            pathVariableName(parameter) != null -> {
               try {
                  val pathVariableName = pathVariableName(parameter)!!
                  val v = request.pathVariable(pathVariableName)
                  v.valueOrRejectIfMissingAndMandatory("Path variable", pathVariableName, parameter)
               } catch (e: IllegalArgumentException) {
                  Mono.error(HttpStatusException(HttpStatus.BAD_REQUEST, e.message!!))
               }
            }

            queryVariableName(parameter) != null -> {
               try {
                  val queryVariableName = queryVariableName(parameter)!!
                  request.queryParam(queryVariableName)
                     .valueOrRejectIfMissingAndMandatory("query variable", queryVariableName, parameter)
               } catch (e: IllegalArgumentException) {
                  Mono.error(HttpStatusException(HttpStatus.BAD_REQUEST, e.message!!))
               }
            }

            headerVariableName(parameter) != null -> {
               try {
                  val headerVariableName = headerVariableName(parameter)!!
                  val headerValue = request.headers().firstHeader(headerVariableName)
                  headerValue.valueOrRejectIfMissingAndMandatory("HTTP header", headerVariableName, parameter)
               } catch (e: IllegalArgumentException) {
                  Mono.error(HttpStatusException(HttpStatus.BAD_REQUEST, e.message!!))
               }
            }

            isRequestBody(parameter) -> {
               request.bodyToMono(String::class.java)
                  .switchIfEmpty {
                     Mono.error(
                        HttpStatusException(
                           HttpStatus.BAD_REQUEST,
                           "Expected a request body, but none was provided"
                        )
                     )
                  }
            }

            responseHeaderName(parameter) != null -> {
               val responseHeaderAnnotation = responseHeaderName((parameter))
               Mono.justOrEmpty(responseHeaderAnnotation!!.value)
            }

            else -> {
               // This is a common error when people forget to add an annotation (or an import
               // for an annotation). So try and detect that case and provide a helpful error
               // message
               val errorMessage = if (!hasAnyExpectedHttpAnnotation(parameter)) {
                  "Parameter '${parameter.name}' needs an annotation to specify how it should be resolved from the request. Consider adding one of ${listOf(
                     HttpHeader.NAME, HttpRequestBody.NAME, HttpQueryVariable.NAME, HttpPathVariable.NAME).joinToString()}. (Check imports if annotation seems present but isn't recognized)."
               } else {
                  "Parameter ${parameter.name} was not provided through the request"
               }
               Mono.error(
                  HttpStatusException(
                     HttpStatus.BAD_REQUEST,
                     errorMessage
                  )
               )
            }
         }
            .map { rawValue ->
               FactValue.Constant(TypedValue(parameter.type, rawValue))
            }

            as Mono<FactValue>
      }

      private fun hasAnyExpectedHttpAnnotation(parameter: Parameter): Boolean {
         return isRequestBody(parameter) || pathVariableName(parameter) != null || queryVariableName(parameter) != null
      }

      private fun isRequestBody(parameter: Parameter): Boolean {
         return parameter.annotation(HttpRequestBody.NAME) != null
      }

      private fun pathVariableName(parameter: Parameter): String? {
         return parameter.annotation(HttpPathVariable.NAME)?.let { it.defaultParameterValue?.toString() }
      }

      private fun queryVariableName(parameter: Parameter): String? {
         return parameter.annotation(HttpQueryVariable.NAME)?.let { it.defaultParameterValue?.toString() }
      }

      private fun headerVariableName(parameter: Parameter): String? {
         return parameter.annotation(HttpHeader.NAME)?.let { it.parameters["name"]?.toString() }
      }

      private fun responseHeaderName(parameter: Parameter): HttpResponseHeader? {
         return parameter.annotation(HttpResponseHeader.NAME)?.let {
            HttpResponseHeader.fromAnnotation(it)
         }
      }
   }
}

private fun <T> Optional<T>.valueOrRejectIfMissingAndMandatory(
   parameterKind: String,
   parameterName: String,
   parameter: Parameter
): Mono<T> {
   return when {
      this.isPresent -> Mono.just(this.get())
      else -> (null as T?).valueOrRejectIfMissingAndMandatory(parameterKind, parameterName, parameter)
   }
}

private fun <T> T?.valueOrRejectIfMissingAndMandatory(
   parameterKind: String,
   parameterName: String,
   parameter: Parameter
): Mono<T> {
   return when {
      this != null -> Mono.just(this)
      parameter.nullable -> {
         Mono.empty()
      }

      else -> Mono.error(
         HttpStatusException(
            HttpStatus.BAD_REQUEST,
            """$parameterKind "$parameterName" was not provided, and is required"""
         )
      )
   }
}
