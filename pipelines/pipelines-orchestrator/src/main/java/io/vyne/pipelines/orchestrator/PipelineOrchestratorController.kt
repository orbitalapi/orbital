package io.vyne.pipelines.orchestrator

import io.swagger.annotations.*
import io.vyne.pipelines.orchestrator.pipelines.InvalidPipelineDescriptionException
import io.vyne.pipelines.orchestrator.pipelines.PipelineDeserialiser
import io.vyne.utils.log
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException


@RestController
@Api(tags = ["Pipeline Orchestrator Controller"], description = "Manage pipelines and runners")

class PipelineOrchestratorController(
   val pipelineManager: PipelinesManager,
   val pipelineDeserialiser: PipelineDeserialiser) : PipelinesOrchestratorApi {

   @ApiOperation("Submit a pipeline")
   @ApiImplicitParams( ApiImplicitParam(value = "pipelineDescription", paramType = "body", dataType = "Pipeline"))
   override fun submitPipeline(
      @RequestBody @ApiParam(hidden = true) pipelineDescription: String
   ): PipelineStateSnapshot {
      log().info("Received submitted pipeline: \n$pipelineDescription")

      return try {
         // Deserialise the full pipeline. We only need the name for now. But it allows us to validate the json and in the future, perform some validations
         val pipeline = pipelineDeserialiser.deserialise(pipelineDescription)

         pipelineManager.addPipeline(PipelineReference(pipeline.name, pipelineDescription))
      } catch (e: InvalidPipelineDescriptionException) {
         throw BadRequestException("Invalid pipeline description", e)
      } catch (e: PipelineAlreadyExistsException) {
         throw BadRequestException("Pipeline is already registered", e)
      } catch (e: Exception) {
         throw BadRequestException("Error while submitting pipeline", e)
      }
   }

   @ApiOperation("Get all pipeline runners")
   override fun getRunners(): List<PipelineRunnerInstance> {

      return try {
         pipelineManager.runnerInstances.map { PipelineRunnerInstance(it.instanceId, it.uri.toString()) }
      } catch (e: Exception) {

         throw BadRequestException("Error while getting instances", e)
      }
   }

   @ApiOperation("Get all pipelines")
   override fun getPipelines(): List<PipelineStateSnapshot> {

      return try {
         pipelineManager.pipelines.map { it.value }
      } catch (e: Exception) {
         throw BadRequestException("Error while getting pipelines", e)
      }
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
