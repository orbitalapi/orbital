package io.vyne.pipelines.orchestrator

import io.vyne.pipelines.orchestrator.pipelines.InvalidPipelineDescriptionException
import io.vyne.pipelines.orchestrator.pipelines.PipelineDeserialiser
import io.vyne.pipelines.orchestrator.runners.PipelineRunnerApi
import io.vyne.utils.log
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.badRequest
import org.springframework.http.ResponseEntity.ok
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class PipelineOrchestratorController(val pipelineDeserialiser: PipelineDeserialiser, val runner: PipelineRunnerApi) {

   @PostMapping("/runner/pipelines")
   fun submitPipeline(@RequestBody pipelineDefinition: String): ResponseEntity<Any> {
      log().info("Received submitted pipeline: \n$pipelineDefinition")

      try {
         // As for now, the output of deserialisation is not used. This is just to ensure we can actually deserialise the pipeline before sending it to any runner
         val pipeline = pipelineDeserialiser.deserialise(pipelineDefinition)

         // TODO : Here, we'd want some way of storing which pipelines are running where.
         // However, ideally, we'd be using Eureka et al to track this in a distributed way, so that
         // instances are reporting which pipelines are running in a distributed manner,
         // which makes recovering from restarts a bit easier.

         // Submit the pipeline to the runner
         val runnerResponse = runner.submitPipeline(pipelineDefinition)
         return ok(runnerResponse)
      } catch (e: InvalidPipelineDescriptionException) {
         return badRequest().body(e.message)
      }


   }
}



