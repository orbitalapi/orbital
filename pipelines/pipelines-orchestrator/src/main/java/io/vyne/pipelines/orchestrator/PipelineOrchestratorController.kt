package io.vyne.pipelines.orchestrator

import io.swagger.annotations.*
import io.vyne.pipelines.orchestrator.pipelines.InvalidPipelineDescriptionException
import io.vyne.pipelines.orchestrator.pipelines.PipelinesService
import io.vyne.utils.log
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException


@RestController
@Api(tags = ["Pipeline Orchestrator Controller"], description = "Manage pipelines and runners")

class PipelineOrchestratorController(
   val pipelinesService: PipelinesService) : PipelinesOrchestratorApi {

   @ApiOperation("Submit a pipeline")
   @ApiImplicitParams( ApiImplicitParam(value = "pipelineDescription", paramType = "body", dataType = "Pipeline"))
   override fun submitPipeline(
      @RequestBody @ApiParam(hidden = true) pipelineDescription: String
   ): PipelineStateSnapshot {
      log().info("Received submitted pipeline: \n$pipelineDescription")

      return try {
         pipelinesService.initialisePipeline(pipelineDescription)
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
         pipelinesService.runners()
      } catch (e: Exception) {

         throw BadRequestException("Error while getting instances", e)
      }
   }

   @ApiOperation("Get all pipelines")
   override fun getPipelines(): List<PipelineStateSnapshot> {

      return try {
         pipelinesService.pipelines()
      } catch (e: Exception) {
         throw BadRequestException("Error while getting pipelines", e)
      }
   }

}

class BadRequestException(message: String, e: Exception? = null) : ResponseStatusException(HttpStatus.BAD_REQUEST, message, e)
