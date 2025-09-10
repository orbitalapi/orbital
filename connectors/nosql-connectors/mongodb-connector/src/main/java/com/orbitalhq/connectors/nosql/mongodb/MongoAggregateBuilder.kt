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
   private val objectMapper: ObjectMapper = jacksonObjectMapper()
) {


   /**
    * Returns the actual aggregation (for executing with Mongo), and the
    * list of documents that describe the aggregation pipeline (for tracing purposes)
    */
   fun buildAggregation(
      steps: List<String>,
      parameters: Map<String, Any?>
   ): Either<MalformedAggregationException, Aggregation> {

      if (steps.isEmpty()) {
         return MalformedAggregationException(
            step = "",
            stepIndex = -1,
            message = "Pipeline cannot be empty"
         ).left()
      }

      val operations = mutableListOf<AggregationOperation>()
      steps.forEachIndexed { index, step ->
         when (val result = buildAggregationOperation(step, parameters, index)) {
            is Either.Left -> return result
            is Either.Right -> {
               val operation = result.value
               operations.add(operation)
            }
         }
      }

      return Aggregation.newAggregation(operations)
         .right()
   }

   private fun buildAggregationOperation(
      step: String,
      parameters: Map<String, Any?>,
      stepIndex: Int
   ): Either<MalformedAggregationException, AggregationOperation> {

      // SECURITY: Replace parameters safely using JSON serialization (no string concatenation)
      val processedStep = BuilderUtils.replaceParametersSafely(step, parameters, stepIndex, objectMapper)
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

      return operation.right()
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


