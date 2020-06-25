package io.vyne.pipelines.orchestrator

import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.orchestrator.pipelines.InvalidPipelineDescriptionException
import io.vyne.pipelines.orchestrator.pipelines.PipelineDeserialiser
import io.vyne.pipelines.orchestrator.runners.PipelineRunnerApi
import io.vyne.pipelines.runner.PipelineInstanceReference
import io.vyne.utils.log
import org.springframework.cloud.client.ServiceInstance
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.ok
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException


@RestController
class PipelineOrchestratorController(val pipelineManager: PipelinesManager, val pipelineDeserialiser: PipelineDeserialiser, val pipelineRunnerApi: PipelineRunnerApi) {

   @PostMapping("/runner/pipelines")
   fun submitPipeline(@RequestBody pipelineDescription: String): Pipeline {
      log().info("Received submitted pipeline: \n$pipelineDescription")

      return try {
         // Deserialise the full pipeline. We only need the name for now. But it allows us to validate the json and in the future, perform some validations
         val pipeline = pipelineDeserialiser.deserialise(pipelineDescription)

         pipelineManager.addPipeline(PipelineReference(pipeline.name, pipelineDescription))
         pipeline
      } catch (e: InvalidPipelineDescriptionException) {
         throw BadRequestException("Invalid pipeline description", e)
      }catch (e: PipelineAlreadyExistsException) {
         throw BadRequestException("Pipeline is already registered", e)
      }catch(e: Exception) {
         throw BadRequestException("Error while submitting pipeline", e)
      }
   }

   @GetMapping("/runners")
   fun getRunners(): ResponseEntity<List<ServiceInstance>> {

      return try {
         val instances = pipelineManager.runnerInstances
         ok(instances)
      } catch (e: Exception) {
         throw BadRequestException("Error while getting instances", e)
      }
   }

   @GetMapping("/pipelines")
   fun getPipelines(): ResponseEntity<List<PipelineStateSnapshot>> {

      return try {
         val pipelines = pipelineManager.pipelines.map { it.value }
         ok(pipelines)
      } catch (e: Exception) {
         throw BadRequestException("Error while getting pipelines", e)
      }
   }

   @GetMapping("/runner/pipelines/{pipelineName}")
   fun getPipeline(@PathVariable pipelineName: String): ResponseEntity<PipelineInstanceReference> {
      return pipelineRunnerApi.getPipeline(pipelineName)
   }
}

class BadRequestException(message: String, e: Exception? = null) : ResponseStatusException(HttpStatus.BAD_REQUEST, message, e) {

   companion object {

      fun throwIf(condition: Boolean, message: String) {
         if (condition) {
            throw BadRequestException(message)
         }
      }
   }
}
