package io.vyne.pipelines.runner.jobs

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.appinfo.ApplicationInfoManager
import io.vyne.pipelines.PIPELINE_METADATA_KEY
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.runner.PipelineBuilder
import io.vyne.pipelines.runner.PipelineInstance
import io.vyne.pipelines.runner.PipelineInstanceReference
import io.vyne.pipelines.runner.PipelineRunnerApi
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}
@RestController
class RunnerJobController(val pipelineStateManager: PipelineStateManager) : PipelineRunnerApi {

   override fun submitPipeline(@RequestBody pipeline: Pipeline): PipelineInstanceReference {
      logger.info { "Received submit Pipeline Request => $pipeline" }
      return pipelineStateManager.registerPipeline(pipeline)
   }

}

@Component
class PipelineStateManager(val appInfoManager: ApplicationInfoManager, val objectMapper: ObjectMapper, private val pipelineBuilder: PipelineBuilder) {

   val pipelines: MutableMap<String, PipelineInstance> = mutableMapOf()

   @Synchronized
   fun registerPipeline(pipeline: Pipeline): PipelineInstance {

      BadRequestException.throwIf(pipelines.filter { it.value.spec.name == pipeline.name }.isNotEmpty(), "Pipeline ${pipeline.name} already running")

      // Build the pipeline
      logger.info { "Building the pipeline => ${pipeline.name} / ${pipeline.id}" }
      val instance = try {
         pipelineBuilder.build(pipeline)
      } catch (ex: Exception) {
         logger.error(ex) { "Error in building pipeline => $pipeline" }
         null
      } ?: throw IllegalStateException("PipelineInstance can't be initialised for ${pipeline.name}")

      // Store it
      pipelines[pipeline.name] = instance

      // Register metadata
      appInfoManager.registerAppMetadata(
         mapOf(
            "$PIPELINE_METADATA_KEY-${pipeline.name}" to objectMapper.writeValueAsString(instance.spec)
         )
      )

      return instance
   }
}

@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadRequestException(message: String) : RuntimeException(message) {
   companion object {

      fun throwIf(condition: Boolean, message: String) {
         if (condition) {
            logger.warn { message }
            throw BadRequestException(message)
         }
      }
   }
}

