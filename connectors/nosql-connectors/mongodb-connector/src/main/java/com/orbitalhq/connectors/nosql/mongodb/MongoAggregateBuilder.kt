package com.orbitalhq.connectors.nosql.mongodb

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.bson.Document
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation

class MalformedAggregationException(
   val step: String,
   val stepIndex: Int,
   override val message: String,
   override val cause: Throwable? = null
) : Exception(message, cause)

class MongoAggregateBuilder(
   objectMapper: ObjectMapper = jacksonObjectMapper()
) {

   private val objectMapper = objectMapper.registerModule(MongoDocumentModule)
   companion object {
      // Validate known stage operators (allow unknown for forward compatibility)
      private val knownStages = setOf(
         "match", "group", "sort", "limit", "skip", "project",
         "unwind", "lookup", "addFields", "count", "facet",
         "sample", "out", "merge", "replaceWith", "replaceRoot",
         "addToSet", "push", "sum", "avg", "first", "last"
      ).map { "\$" + it } // Mongo stages all start with a $ symbol (eg: $match)

   }

   /**
    * Returns the actual aggregation (for executing with Mongo), and the
    * list of documents that describe the aggregation pipeline (for tracing purposes)
    */
   fun buildAggregation(
      steps: List<String>,
      parameters: Map<String, Any?>
   ): Either<MalformedAggregationException, Pair<Aggregation, List<Document>>> {

      if (steps.isEmpty()) {
         return MalformedAggregationException(
            step = "",
            stepIndex = -1,
            message = "Pipeline cannot be empty"
         ).left()
      }

      val operations = mutableListOf<AggregationOperation>()
      val documents = mutableListOf<Document>()
      steps.forEachIndexed { index, step ->
         when (val result = buildAggregationOperation(step, parameters, index)) {
            is Either.Left -> return result
            is Either.Right -> {
               val (operation, document) = result.value
               operations.add(operation)
               documents.add(document)
            }
         }
      }

      return (Aggregation.newAggregation(operations) to documents).right()
   }

   private fun buildAggregationOperation(
      step: String,
      parameters: Map<String, Any?>,
      stepIndex: Int
   ): Either<MalformedAggregationException, Pair<AggregationOperation,Document>> {

      // SECURITY: Replace parameters safely using JSON serialization (no string concatenation)
      val processedStep = replaceParametersSafely(step, parameters, stepIndex)
         .fold(
            { error -> return error.left() },
            { processed -> processed }
         )

      // Validate the final JSON structure
      val document = try {
         Document.parse(processedStep)
      } catch (e: Exception) {
         return MalformedAggregationException(
            step = step,
            stepIndex = stepIndex,
            message = "Invalid JSON in aggregation step after parameter replacement: ${e.message}",
            cause = e
         ).left()
      }

      // Validate it's a proper MongoDB aggregation stage
      validateAggregationStage(document, step, stepIndex)?.let { error ->
         return error.left()
      }

      // Create the aggregation operation
      val operation = AggregationOperation { document }

      return (operation to document).right()
   }

   /**
    * SECURITY-CRITICAL: Safely replace parameters using JSON serialization
    * This prevents injection by never doing string concatenation of user input
    */
   private fun replaceParametersSafely(
      step: String,
      parameters: Map<String, Any?>,
      stepIndex: Int
   ): Either<MalformedAggregationException, String> {

      val parameterPattern = Regex(":([a-zA-Z_][a-zA-Z0-9_]*)")
      val foundParameters = parameterPattern.findAll(step).map { it.groupValues[1] }.toSet()

      // Check for missing parameters
      val missingParams = foundParameters - parameters.keys
      if (missingParams.isNotEmpty()) {
         return MalformedAggregationException(
            step = step,
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
               step = step,
               stepIndex = stepIndex,
               message = "Failed to serialize parameter '$paramName': ${e.message}",
               cause = e
            ).left()
         }
      }

      // Replace parameters with safe serialized values
      var result = step
      safeReplacements.forEach { (paramName, safeValue) ->
         result = result.replace(":$paramName", safeValue)
      }

      // SECURITY: Verify no parameters remain unreplaced (defense in depth)
      if (parameterPattern.containsMatchIn(result)) {
         val remaining = parameterPattern.findAll(result).map { it.groupValues[1] }.toSet()
         return MalformedAggregationException(
            step = step,
            stepIndex = stepIndex,
            message = "Internal error: Parameters not fully replaced: ${remaining.joinToString(", ")}"
         ).left()
      }

      return result.right()
   }

   private fun validateAggregationStage(
      document: Document,
      originalStep: String,
      stepIndex: Int
   ): MalformedAggregationException? {

      if (document.isEmpty()) {
         return MalformedAggregationException(
            step = originalStep,
            stepIndex = stepIndex,
            message = "Aggregation step cannot be empty"
         )
      }

      val fieldNames = document.keys.toList()

      // Check if it looks like a valid MongoDB stage (starts with $)
      val stageOperators = fieldNames.filter { it.startsWith("$") }
      if (stageOperators.isEmpty()) {
         return MalformedAggregationException(
            step = originalStep,
            stepIndex = stepIndex,
            message = "Aggregation step must contain at least one MongoDB stage operator (starting with \$)"
         )
      }


      return null
   }
}



/**
 * Jackson module for MongoDB Document serialization/deserialization.
 * Serialization defers to Document.toJson() for proper BSON handling.
 */
private object MongoDocumentModule : SimpleModule("MongoDocumentModule") {

   init {
      addSerializer(Document::class.java, DocumentSerializer())
   }
}

/**
 * Serializer that delegates to Document.toJson() for proper BSON type handling
 */
private class DocumentSerializer : JsonSerializer<Document>() {

   override fun serialize(document: Document?, gen: JsonGenerator, serializers: SerializerProvider) {
      if (document == null) {
         gen.writeNull()
         return
      }

      // Use Document.toJson() which properly handles BSON types like ObjectId, Date, etc.
      val json = document.toJson()
      gen.writeRawValue(json)
   }
}
