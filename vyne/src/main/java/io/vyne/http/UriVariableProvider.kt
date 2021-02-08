package io.vyne.http

import io.vyne.models.TypedInstance
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

class UriVariableProvider {
   private val NAMES_REGEX = "\\{([^/]+?)}".toRegex()

   fun findVariableNames(url: String): List<String> {
      return NAMES_REGEX.findAll(url).map { it.groupValues[1] }.toList()
   }

   fun getUriVariables(parameters: List<ParameterValuePair>, url: String): Map<String, Any> {
      return findVariableNames(url).map { name ->
         val parameterValuePair = parameters.findByTypeName(name)
            ?: parameters.findByParameterName(name)
            ?: error("No argument provided for url variable $name")
         name to (parameterValuePair.second.value ?:
         error("Error constructing url $url, found null for parameter $name"))
      }.toMap()
   }

   private fun List<ParameterValuePair>.findByTypeName(typeName: String): ParameterValuePair? {
      return this.firstOrNull { it.first.type.fullyQualifiedName == typeName }
   }

   private fun List<ParameterValuePair>.findByParameterName(parameterName: String): ParameterValuePair? {
      return this.firstOrNull { it.first.isNamed(parameterName) }
   }

   companion object {
       fun buildRequestBody(operation: RemoteOperation, parameters: List<TypedInstance>): Pair<HttpEntity<*>, Class<*>> {
         if (operation.hasMetadata("HttpOperation")) {
            // TODO Revisit as this is a quick hack to invoke services that returns simple/text
            val httpOperation = operation.metadata("HttpOperation")
            httpOperation.params["consumes"]?.let {
               val httpHeaders = HttpHeaders()
               httpHeaders.accept = mutableListOf(MediaType.parseMediaType(it as String))
               return HttpEntity<String>(httpHeaders) to String::class.java
            }
         }
         val requestBodyParamIdx = operation.parameters.indexOfFirst { it.hasMetadata("RequestBody") }
         if (requestBodyParamIdx == -1) return HttpEntity.EMPTY to Any::class.java
         // TODO : For now, only looking up param based on type.  This is obviously naieve, and should
         // be improved, using name / position?  (note that parameters don't appear to be ordered in the list).

         val requestBodyParamType = operation.parameters[requestBodyParamIdx].type
         val requestBodyTypedInstance = parameters.first { it.type.name == requestBodyParamType.name }
         return HttpEntity(requestBodyTypedInstance.toRawObject()) to Any::class.java

      }
   }
}
typealias ParameterValuePair = Pair<Parameter, TypedInstance>
