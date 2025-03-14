package com.orbitalhq.http

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schemas.Parameter
import lang.taxi.annotations.HttpPathVariable

class UriVariableProvider {
   private val NAMES_REGEX = "\\{([^/]+?)}".toRegex()

   fun findVariableNames(url: String): List<String> {
      return NAMES_REGEX.findAll(url).map { it.groupValues[1] }.toList()
   }

   fun getUriVariables(parameters: List<ParameterValuePair>, url: String): Map<String, Any> {
      return findVariableNames(url).map { name ->
         val parameterValuePair =
            parameters.findByPathVariableWithName(name)
               ?: parameters.findByTypeName(name)
               ?: parameters.findByParameterName(name)
               ?: error("No argument provided for url variable $name")
         if (parameterValuePair.second.value == null) {
            println()
         }
         name to (parameterValuePair.second.value
            ?: error("Error constructing url $url, found null for parameter $name"))
      }.toMap()
   }

   private fun List<ParameterValuePair>.findByTypeName(typeName: String): ParameterValuePair? {
      return this.firstOrNull { it.first.type.fullyQualifiedName == typeName }
   }

   private fun List<ParameterValuePair>.findByParameterName(parameterName: String): ParameterValuePair? {
      return this.firstOrNull { it.first.isNamed(parameterName) }
   }

   private fun List<ParameterValuePair>.findByPathVariableWithName(pathVariableName: String): ParameterValuePair? {
      return this.firstOrNull { (parameter, typedInstance) ->
         parameter.allMetadata(HttpPathVariable.NAME).any { it.params["value"] == pathVariableName}
      }
   }

}
typealias ParameterValuePair = Pair<Parameter, TypedInstance>
