package io.vyne.pipelines.orchestrator

//import io.swagger.annotations.*
import io.vyne.pipelines.orchestrator.pipelines.InvalidPipelineDescriptionException
import io.vyne.pipelines.orchestrator.pipelines.PipelinesService
import io.vyne.pipelines.runner.PipelineStatusUpdate
import io.vyne.utils.log
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono


@RestController
//@Api(tags = ["Pipeline Orchestrator Controller"], description = "Manage pipelines and runners")

class PipelineOrchestratorController(
   val pipelinesService: PipelinesService
) : PipelinesOrchestratorApi {
   private val logger = KotlinLogging.logger {}

   //@ApiOperation("Submit a pipeline")
   //@ApiImplicitParams( ApiImplicitParam(value = "pipelineDescription", paramType = "body", dataType = "Pipeline"))
   override fun submitPipeline(
      @RequestBody pipelineDescription: String //@ApiParam(hidden = true)
   ): Mono<PipelineStateSnapshot> {
      logger.info("Received submitted pipeline: \n$pipelineDescription")

      return try {
         Mono.just(pipelinesService.initialisePipeline(pipelineDescription))
      } catch (e: InvalidPipelineDescriptionException) {
         logger.error(e) { "Failed to parse pipeline JSON" }
         throw BadRequestException("Invalid pipeline JSON - ${e.message}", e)
      } catch (e: PipelineAlreadyExistsException) {
         logger.error(e) { "Pipeline already exists" }
         throw BadRequestException("Pipeline is already registered", e)
      } catch (e: Exception) {
         logger.error(e) { "Error whilst submitting pipeline" }
         throw BadRequestException("Error while submitting pipeline", e)
      }
   }

   //@ApiOperation("Get all pipeline runners")
   override fun getRunners(): Mono<List<PipelineRunnerInstance>> {

      return try {
         Mono.just(pipelinesService.runners())
      } catch (e: Exception) {

         throw BadRequestException("Error while getting instances", e)
      }
   }

   //@ApiOperation("Get all pipelines")
   override fun getPipelines(): Mono<List<PipelineStateSnapshot>> {

      return try {
         Mono.just(pipelinesService.pipelines())
      } catch (e: Exception) {
         throw BadRequestException("Error while getting pipelines", e)
      }
   }

   override fun removePipeline(pipelineName: String): Mono<PipelineStatusUpdate> {
      log().info("Received request to remove pipeline $pipelineName")
      return pipelinesService.removePipeline(pipelineName)
   }

}

class BadRequestException(message: String, e: Exception? = null) :
   ResponseStatusException(HttpStatus.BAD_REQUEST, message, e)
