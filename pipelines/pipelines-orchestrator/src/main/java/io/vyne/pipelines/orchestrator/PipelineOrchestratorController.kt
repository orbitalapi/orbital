package io.vyne.pipelines.orchestrator

import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.orchestrator.pipelines.InvalidPipelineDescriptionException
import io.vyne.pipelines.orchestrator.pipelines.PipelineDeserialiser
import io.vyne.utils.log
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.ok
import org.springframework.web.bind.annotation.*
import java.lang.RuntimeException


@RestController
class PipelineOrchestratorController(val pipelineManager: PipelinesManager, val pipelineDeserialiser: PipelineDeserialiser) {

   @PostMapping("/runner/pipelines")
   fun submitPipeline(@RequestBody pipelineDescription: String): ResponseEntity<Pipeline> {
      log().info("Received submitted pipeline: \n$pipelineDescription")

      return try {
         // Deserialise the full pipeline. We only need the name for now. But it allows us to validate the json and in the future, perform some validations
         val pipeline = pipelineDeserialiser.deserialise(pipelineDescription)

         pipelineManager.addPipeline(PipelineReference(pipeline.name, pipelineDescription))
         ok(pipeline)
      } catch (e: InvalidPipelineDescriptionException) {
         throw BadRequestException("Invalid pipeline description", e)
      }
   }

   @GetMapping("/runners")
   fun getInstances(): ResponseEntity<Any> {

      return try {
         val instances = pipelineManager.runnerInstances
         ok(instances)
      } catch (e: Exception) {
         throw BadRequestException("Error while getting instances", e)
      }
   }

   @GetMapping("/pipelines")
   fun getPipelines(): ResponseEntity<Any> {

      return try {
         val pipelines = pipelineManager.pipelines.map { it.value }
         ok(pipelines)
      } catch (e: Exception) {
         throw BadRequestException("Error while getting pipelines", e)
      }
   }
}

@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadRequestException(message: String, e: Exception? = null) : RuntimeException(message, e) {

   companion object {

      fun throwIf(condition: Boolean, message: String) {
         if (condition) {
            throw BadRequestException(message)
         }
      }
   }
}
