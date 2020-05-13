package io.vyne.pipelines.orchestrator.pipelines

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.pipelines.Pipeline
import org.springframework.stereotype.Component

@Component
class PipelineDeserialiser(val objectMapper: ObjectMapper) {


   /**
    * Deserialises and validates a string pipeline description
    */
   fun deserialise(pipeline: String): Pipeline {
      try {
         // ENHANCE add any validation here
         return objectMapper.readValue(pipeline, Pipeline::class.java)
      } catch (e: JsonProcessingException) {
         throw InvalidPipelineDescriptionException("Could not deserialise pipeline: ${e.message}", e)
      }
   }

}

class InvalidPipelineDescriptionException(message: String, throwable: Throwable) : Exception(message, throwable)
