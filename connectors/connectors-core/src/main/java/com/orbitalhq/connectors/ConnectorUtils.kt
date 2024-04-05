package com.orbitalhq.connectors

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.ConstructedQueryDataSource
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schemas.Parameter
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery


object ConnectorUtils {
   private fun findMissingParameters(
      parameters: List<ConnectionDriverParam>,
      inputs: Map<ConnectionParameterName, Any?>
   ): List<ConnectionDriverParam> {
      return parameters.filter { it.required }
         .filter { !inputs.containsKey(it.templateParamName) || (inputs.containsKey(it.templateParamName) && inputs[it.templateParamName] == null) }
   }
   fun assertAllParametersPresent(
      parameters: List<ConnectionDriverParam>,
      inputs: Map<ConnectionParameterName, Any?>
   ): Map<ConnectionParameterName, Any> {
      val missing = findMissingParameters(parameters, inputs)
      val missingWithoutDefault = missing.filter { it.defaultValue == null }
      if (missingWithoutDefault.isNotEmpty()) {
         throw MissingConnectionParametersException(missingWithoutDefault)
      }
      return (inputs.filter { it.value != null } as Map<ConnectionParameterName, Any>) + missing.map { it.templateParamName to it.defaultValue!! }
   }
}
fun Array<out IConnectionParameter>.connectionParams(): List<ConnectionDriverParam> = this.map { it.param }

class MissingConnectionParametersException(private val parameters: List<ConnectionDriverParam>) :
   RuntimeException("The following parameters were not provided: ${parameters.joinToString { it.displayName }}")


fun Map<ConnectionParameterName, String>.valueOrThrowNiceMessage(key:String):String {
   return this[key] ?: error("Connection parameter '$key' was not defined, but is required")
}

fun List<Pair<Parameter, TypedInstance>>.getTaxiQlQuery():Pair<TaxiQLQueryString,ConstructedQueryDataSource> {
   require(this.size == 1) { "Parameters are expected to have a single param when invoking a TaxiQL query service, but found ${this.size}"}
   val (param,instance) = this.single()
   require(param.type.qualifiedName.parameterizedName == VyneQlGrammar.QUERY_TYPE_NAME) { "Expected to find a TaxiQL query, but found instance of ${param.type.qualifiedName.parameterizedName}"}
   return instance.value as String to instance.source as ConstructedQueryDataSource
}
