package io.vyne.spring.invokers

import java.util.regex.Pattern

class UriVariableProvider {
   private val NAMES_PATTERN = Pattern.compile("\\{([^/]+?)\\}")
   private val NAMES_REGEX = "\\{([^/]+?)}".toRegex()

   fun findVariableNames(url: String): List<String> {
      return NAMES_REGEX.findAll(url).map { it.groupValues[1] }.toList()
   }

   fun getUriVariables(parameters: List<ParameterValuePair>, url: String): Map<String, Any> {
      return findVariableNames(url).map { name ->
         val parameterValuePair = parameters.findByTypeName(name)
            ?: parameters.findByParameterName(name)
            ?: error("No argument provided for url variable $name")
         name to parameterValuePair.second.value
      }.toMap()
   }

   private fun List<ParameterValuePair>.findByTypeName(typeName: String): ParameterValuePair? {
      return this.firstOrNull { it.first.type.fullyQualifiedName == typeName }
   }

   private fun List<ParameterValuePair>.findByParameterName(parameterName: String): ParameterValuePair? {
      return this.firstOrNull { it.first.isNamed(parameterName) }
   }

}
