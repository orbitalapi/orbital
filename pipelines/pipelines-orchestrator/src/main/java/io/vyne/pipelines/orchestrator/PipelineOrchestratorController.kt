package io.vyne.pipelines.orchestrator

import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.orchestrator.pipelines.InvalidPipelineDescriptionException
import io.vyne.utils.log
import org.springframework.cloud.client.ServiceInstance
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.badRequest
import org.springframework.http.ResponseEntity.ok
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController


@RestController
class PipelineOrchestratorController(val pipelineManager: PipelinesManager) {

   @PostMapping("/runner/pipelines")
   fun submitPipeline(@RequestBody pipelineDefinition: String): ResponseEntity<Any> {
      log().info("Received submitted pipeline: \n$pipelineDefinition")

      return try {
         var pipeline = pipelineManager.addPipeline(pipelineDefinition)
         ok(pipeline)
      } catch (e: InvalidPipelineDescriptionException) {
         badRequest().body(e.message)
      }
   }

   @GetMapping("/runners")
   fun getInstances(): ResponseEntity<Any> {

      return try {
         var instances = pipelineManager.runnerInstances
         ok(instances)
      } catch (e: Exception) {
         badRequest().body(e.message)
      }
   }

   @GetMapping("/pipelines")
   fun getPipelines(): ResponseEntity<Any> {

      return try {
         val pipelines = pipelineManager.pipelines.map { it.value }
         ok(pipelines)
      } catch (e: Exception) {
         badRequest().body(e.message)
      }
   }
}










