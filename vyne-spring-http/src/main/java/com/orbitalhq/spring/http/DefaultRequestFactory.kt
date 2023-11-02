package com.orbitalhq.spring.http

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schemas.RemoteOperation
import lang.taxi.annotations.HttpOperation
import lang.taxi.annotations.HttpRequestBody
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.util.MultiValueMap

interface HttpRequestFactory {
   fun buildRequestBody(operation: RemoteOperation, parameters: List<TypedInstance>): HttpEntity<*>
   fun buildRequestQueryParams(operation: RemoteOperation): MultiValueMap<String, String>? = null
}

class DefaultRequestFactory : HttpRequestFactory {
   override fun buildRequestBody(operation: RemoteOperation, parameters: List<TypedInstance>): HttpEntity<*> {
      if (operation.hasMetadata(HttpOperation.NAME)) {
         // TODO Revisit as this is a quick hack to invoke services that returns simple/text
         val httpOperation = operation.metadata(HttpOperation.NAME)
         httpOperation.params["consumes"]?.let {
            val httpHeaders = HttpHeaders()
            httpHeaders.accept = mutableListOf(MediaType.parseMediaType(it as String))
            return HttpEntity<String>(httpHeaders)
         }
      }
      val requestBodyParamIdx = operation.parameters.indexOfFirst { it.hasMetadata(HttpRequestBody.NAME) }
      if (requestBodyParamIdx == -1) return HttpEntity.EMPTY
      // TODO : For now, only looking up param based on type.  This is obviously naieve, and should
      // be improved, using name / position?  (note that parameters don't appear to be ordered in the list).

      val requestBodyParamType = operation.parameters[requestBodyParamIdx].type
      val requestBodyTypedInstance = parameters.first { it.type.name == requestBodyParamType.name }
      return HttpEntity(requestBodyTypedInstance.toRawObject())

   }
}
