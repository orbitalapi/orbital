package com.orbitalhq.connectors.nosql.mongodb

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.connectors.nosql.mongodb.MongoNativeAggregateQueryInvoker.Companion
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.RemoteOperation
import mu.KotlinLogging

object BuilderUtils {
   private val logger = KotlinLogging.logger {}
   /**
    * SECURITY-CRITICAL: Safely replace parameters using JSON serialization
    * This prevents injection by never doing string concatenation of user input
    */
   fun replaceParametersSafely(
      template: String,
      parameters: Map<String, Any?>,
      stepIndex: Int,
      objectMapper: ObjectMapper
   ): Either<MalformedAggregationException, String> {

      val parameterPattern = Regex(":([a-zA-Z_][a-zA-Z0-9_]*)")
      val foundParameters = parameterPattern.findAll(template).map { it.groupValues[1] }.toSet()

      // Check for missing parameters
      val missingParams = foundParameters - parameters.keys
      if (missingParams.isNotEmpty()) {
         return MalformedAggregationException(
            step = template,
            stepIndex = stepIndex,
            message = "Missing parameters: ${missingParams.joinToString(", ")}"
         ).left()
      }

      // SECURITY: Build a map of safe replacements using JSON serialization
      val safeReplacements = foundParameters.associateWith { paramName ->
         val paramValue = parameters[paramName]
         try {
            // Use Jackson to properly escape/serialize the value
            when (paramValue) {
               null -> "null"
               is String -> objectMapper.writeValueAsString(paramValue) // Properly escapes quotes, etc.
               is Number, is Boolean -> paramValue.toString() // Safe primitive types
               else -> objectMapper.writeValueAsString(paramValue) // Complex objects as JSON
            }
         } catch (e: Exception) {
            return MalformedAggregationException(
               step = template,
               stepIndex = stepIndex,
               message = "Failed to serialize parameter '$paramName': ${e.message}",
               cause = e
            ).left()
         }
      }

      // Replace parameters with safe serialized values
      var result = template
      safeReplacements.forEach { (paramName, safeValue) ->
         result = result.replace(":$paramName", safeValue)
      }

      // SECURITY: Verify no parameters remain unreplaced (defense in depth)
      if (parameterPattern.containsMatchIn(result)) {
         val remaining = parameterPattern.findAll(result).map { it.groupValues[1] }.toSet()
         return MalformedAggregationException(
            step = template,
            stepIndex = stepIndex,
            message = "Internal error: Parameters not fully replaced: ${remaining.joinToString(", ")}"
         ).left()
      }

      return result.right()
   }

   fun extractTemplateParameters(
      parameters: List<Pair<Parameter, TypedInstance>>,
      operation: RemoteOperation
   ): Map<String, Any?> {
      val aggregateParams = parameters.mapIndexedNotNull { index, (param, instance) ->
         if (param.name == null) {
            logger.warn { "Parameter $index on operation ${operation.name} is not named. This cannot be used for supplying values into a Mongo aggregation template." }
            return@mapIndexedNotNull null
         }
         val name = param.name!!
         val value = instance.toRawObject()
         name to value
      }
      return aggregateParams.toMap()
   }

}
