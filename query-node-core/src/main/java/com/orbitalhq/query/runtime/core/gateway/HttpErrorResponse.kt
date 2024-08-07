package com.orbitalhq.query.runtime.core.gateway

import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.errors.ErrorType
import com.orbitalhq.errors.OrbitalQueryException
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.json.Jackson
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.fqn
import lang.taxi.annotations.HttpService
import lang.taxi.annotations.HttpService.Companion.RESPONSE_BODY_TYPE_NAME
import lang.taxi.types.ObjectType
import mu.KotlinLogging
import org.springframework.http.HttpStatus

object HttpErrorResponse {
   private val logger = KotlinLogging.logger {}
   fun getErrorCodeAndPayload(
      error: TypedInstance,
      objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
   ): Pair<HttpStatus, Any> {

      val responseCode = if (error.type.hasMetadata(HttpService.RESPONSE_CODE_TYPE_NAME.fqn())) {
         val metadata = error.type.getMetadata(HttpService.RESPONSE_CODE_TYPE_NAME.fqn())
         metadata.params["value"]?.let {
            HttpStatus.valueOf(it as Int)
         } ?: HttpStatus.BAD_REQUEST
      } else {
         HttpStatus.BAD_REQUEST
      }

      val payload = getErrorPayload(error, objectMapper)
      return responseCode to payload
   }

   private fun getErrorPayload(error: TypedInstance, objectMapper: ObjectMapper): Any {

      fun jsonIfObject(value: Any?):String {
         return when (value) {
            is String,
            is Int,
            is Boolean -> value.toString()
            else -> objectMapper.writeValueAsString(value)
         }
      }

      // If the entire error is tagged as ResponseBody, just JSON the whole thing
      if (error.type.hasMetadata(RESPONSE_BODY_TYPE_NAME.fqn())) {
         return jsonIfObject(error.toRawObject())
      }

      val responseBodyAttributes = error.type.getAttributesWithAnnotation(RESPONSE_BODY_TYPE_NAME.fqn())
      if (responseBodyAttributes.size > 1) {
         logger.warn { "Type ${error.type.paramaterizedName} has ${responseBodyAttributes.size} attributes with @ResponseBody - expected either 0 or 1. Using the first one" }
      }
      responseBodyAttributes.entries.singleOrNull()?.let { (fieldName, field) ->
         val field = (error as TypedObject)[fieldName]
         return jsonIfObject(field.toRawObject())
      }

      if (error is TypedObject) {
         val errorMessageAttributes =
            // Note: Can't use isAssignableTo() here, as we don't
            // have reference to a schema at this point of the call stack
            error.type.attributes.entries.filter { (a, b ) -> b.type.parameterizedName == ErrorType.ErrorMessageQualifiedName.parameterizedName }
         if (errorMessageAttributes.isEmpty()) {
            logger.warn { "Error type ${error.type.paramaterizedName} has no way of exposing an error message or body. Either add a @ResponseBody annotation to the type, one of it's fields, or define an attribute of type ${ErrorType.ErrorMessageQualifiedName.parameterizedName}" }
            return error.type.name.shortDisplayName
         }
         if (errorMessageAttributes.size > 1) {
            logger.warn { "Error type ${error.type.paramaterizedName} has multiple attributed of type ${ErrorType.ErrorMessageQualifiedName.parameterizedName}, expected just one. Picking one." }
         }
         val (errorMessageFieldName) = errorMessageAttributes.first()


         val errorMessage = error[errorMessageFieldName].toRawObject()
         return jsonIfObject(errorMessage)
      }

      // Last ditch, just JSONify the whole object
      return jsonIfObject(error.toRawObject())
   }

   fun getErrorCodeAndPayload(
      exception: OrbitalQueryException,
      objectMapper: ObjectMapper = Jackson.defaultObjectMapper,

   ): Pair<HttpStatus, Any> {
      return getErrorCodeAndPayload(exception.error, objectMapper)
   }
}
