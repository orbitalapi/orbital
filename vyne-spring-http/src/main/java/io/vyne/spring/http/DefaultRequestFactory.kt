package io.vyne.spring.http

import io.vyne.models.TypedInstance
import io.vyne.schemas.RemoteOperation
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

interface HttpRequestFactory {
   fun buildRequestBody(operation: RemoteOperation, parameters: List<TypedInstance>): HttpEntity<*>
}

class DefaultRequestFactory : HttpRequestFactory {
   override fun buildRequestBody(operation: RemoteOperation, parameters: List<TypedInstance>): HttpEntity<*> {
      if (operation.hasMetadata("HttpOperation")) {
         // TODO Revisit as this is a quick hack to invoke services that returns simple/text
         val httpOperation = operation.metadata("HttpOperation")
         httpOperation.params["consumes"]?.let {
            val httpHeaders = HttpHeaders()
            httpHeaders.accept = mutableListOf(MediaType.parseMediaType(it as String))
            return HttpEntity<String>(httpHeaders)
         }
      }
      val requestBodyParamIdx = operation.parameters.indexOfFirst { it.hasMetadata("RequestBody") }
      if (requestBodyParamIdx == -1) return HttpEntity.EMPTY
      // TODO : For now, only looking up param based on type.  This is obviously naieve, and should
      // be improved, using name / position?  (note that parameters don't appear to be ordered in the list).

      val requestBodyParamType = operation.parameters[requestBodyParamIdx].type
      val requestBodyTypedInstance = parameters.first { it.type.name == requestBodyParamType.name }
      return HttpEntity(requestBodyTypedInstance.toRawObject())

   }
}
