package com.orbitalhq.spring.http

import com.google.common.collect.ArrayListMultimap
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.format.FormatDetector
import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.RemoteOperation
import lang.taxi.annotations.HttpHeader
import lang.taxi.annotations.HttpQueryVariable
import lang.taxi.annotations.HttpRequestBody
import mu.KotlinLogging
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap

interface HttpRequestFactory {
   fun buildRequestBody(operation: RemoteOperation, parameters: List<TypedInstance>): HttpEntity<*>
   fun buildRequestQueryParams(parametersAndValues: List<Pair<Parameter, TypedInstance>>): MultiValueMap<String, String>? = null
}

class DefaultRequestFactory(private val formatSpecs: List<ModelFormatSpec>) : HttpRequestFactory {
   init {
      logger.info { "created" }
   }
   companion object {
      private val logger = KotlinLogging.logger {}

   }

   override fun buildRequestBody(operation: RemoteOperation, parameters: List<TypedInstance>): HttpEntity<*> {
      val headers = buildHttpHeaders(operation, parameters)
      val requestBodyParamIdx = operation.parameters.indexOfFirst { it.hasMetadata(HttpRequestBody.NAME) }
      if (requestBodyParamIdx == -1) return HttpEntity<Any>(headers)
      // TODO : For now, only looking up param based on type.  This is obviously naieve, and should
      // be improved, using name / position?  (note that parameters don't appear to be ordered in the list).

      val requestBodyParamType = operation.parameters[requestBodyParamIdx].type

      val requestBodyTypedInstance = parameters.first { it.type.name == requestBodyParamType.name }
      val httpBody = FormatDetector.get(formatSpecs).getFormatType(requestBodyParamType)?.let {
         it.second.serializer.write(requestBodyTypedInstance.toRawObject(), it.first, 0)
      } ?: requestBodyTypedInstance.toRawObject()
      return HttpEntity(httpBody, headers)

   }

   override fun buildRequestQueryParams(parametersAndValues: List<Pair<Parameter, TypedInstance>>): MultiValueMap<String, String>? {
      val queryParamsAndValues = parametersAndValues
         .filter { parameterAndValue -> parameterAndValue.first.hasMetadata(HttpQueryVariable.NAME) }

      return if (queryParamsAndValues.isEmpty()) {
         null
      } else {
         val queryParamMultiMap =  LinkedMultiValueMap<String, String>()
         queryParamsAndValues
            .filter { it.second.value != null }
            .forEach { queryParamsAndValue ->
               queryParamMultiMap.add(queryParamsAndValue.first.name!!, queryParamsAndValue.second.value!!.toString())
            }
         queryParamMultiMap
      }

   }

   private fun buildHttpHeaders(operation: RemoteOperation, parameters: List<TypedInstance>): HttpHeaders {
      val headers = ArrayListMultimap.create<String, String>()
      operation.allMetadata(HttpHeader.NAME).map { HttpHeader.fromMap(it.params) }.forEach { header ->
         headers.put(header.name, header.asValue())
      }
      operation.parameters.forEachIndexed {  index, parameter ->
         if (parameter.hasMetadata(HttpHeader.NAME)) {
            val annotation = HttpHeader.fromMap(parameter.firstMetadata(HttpHeader.NAME).params)
            val typedInstance = parameters[index]
            if (!typedInstance.type.isAssignableTo(parameter.type)) {
               logger.error { "Failed to build headers for operation ${operation.name} - Parameter at index $index was expected to be type ${parameter.type.name.shortDisplayName} but was ${typedInstance.type.name.shortDisplayName}" }
            }

            val headerValue = typedInstance.toRawObject()?.toString() ?: ""
            headers.put(annotation.name, annotation.asValue(headerValue))
         }
      }

      val httpHeaders = HttpHeaders()
      headers.asMap().forEach {  (headerName, value) ->
         httpHeaders[headerName] = value.toList()
      }
      return httpHeaders
   }
}
