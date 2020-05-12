package io.vyne.pipelines.orchestrator

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.orchestrator.runners.PipelineRunnerApi
import io.vyne.utils.log
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.badRequest
import org.springframework.http.ResponseEntity.ok
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class PipelineOrchestratorController(val runner: PipelineRunnerApi, val objectMapper: ObjectMapper) {

   @PostMapping("/runner/pipelines")
   fun submitPipeline(@RequestBody pipelineDefinition: String): ResponseEntity<String> {
      log().info("Received submitted pipeline: \n$pipelineDefinition")

      try {
         // Get the pipeline object from String
         val pipeline = objectMapper.readValue(pipelineDefinition, Pipeline::class.java)

         // TODO : Here, we'd want some way of storing which pipelines are running where.
         // However, ideally, we'd be using Eureka et al to track this in a distributed way, so that
         // instances are reporting which pipelines are running in a distributed manner,
         // which makes recovering from restarts a bit easier.

         // Submit the pipeline to the runner
         // We need to submit the full string and not the deserialized Pipeline object or we might lose information if the Input or the Output types are not known by the Orchestrator
         var runnerResponse = runner.submitPipeline(pipelineDefinition)
         return ok(runnerResponse)

      } catch (e: Exception) {
         return when (e) {
            is JsonProcessingException -> badRequest().body(e.message)
            else -> throw e
         }
      }

   }
}



