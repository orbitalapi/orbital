package io.vyne.query.runtime.core.gateway

import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.TaxiDocument
import lang.taxi.annotations.HttpPathVariable
import lang.taxi.query.FactValue
import lang.taxi.query.Parameter
import lang.taxi.query.TaxiQlQuery
import lang.taxi.types.TypedValue
import lang.taxi.types.annotation
import org.springframework.web.reactive.function.server.ServerRequest

/**
 * A query that has matched a route.
 * Contains the query itself, and the parameter facts
 * extracted from the inbound request
 */
data class RoutedQuery(
   val query: TaxiQlQuery,
   val parameters: Map<Parameter, FactValue>
) {
   companion object {
      fun build(query: TaxiQlQuery, request: ServerRequest): RoutedQuery {
         val parameters = query.parameters.map { parameter ->
            parameter to extractParameterValueFromRequest(parameter, request)
         }.toMap()
         return RoutedQuery(query, parameters)
      }


      private fun extractParameterValueFromRequest(parameter: Parameter, request: ServerRequest): FactValue {
         val rawValue = when {
            pathVariableName(parameter) != null -> request.pathVariable(pathVariableName(parameter))
            // TODO : RequestBody, and possibly some others?

            // TODO : This should result in a BadRequest, somehow...
            else -> error("Parameter ${parameter.name} was not provided through the request")
         }
         return FactValue.Constant(TypedValue(parameter.type,rawValue))
      }

      private fun pathVariableName(parameter: Parameter): String? {
         return parameter.annotation(HttpPathVariable.NAME)?.let { it.defaultParameterValue?.toString() }
      }
   }
}
