package io.vyne.pipelines.runner.jobs

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.appinfo.ApplicationInfoManager
import io.vyne.pipelines.PIPELINE_METADATA_KEY
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.runner.PipelineBuilder
import io.vyne.pipelines.runner.PipelineInstance
import io.vyne.pipelines.runner.PipelineInstanceReference
import io.vyne.pipelines.runner.PipelineRunnerApi
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import java.lang.RuntimeException

@RestController
class RunnerJobController(val pipelineStateManager: PipelineStateManager) : PipelineRunnerApi {

   override fun submitPipeline(@RequestBody pipeline: Pipeline): PipelineInstanceReference {
      return pipelineStateManager.registerPipeline(pipeline)
   }

   @GetMapping("/runner/pipelines/{id}")
   fun getPipeline(@PathVariable id: String): ResponseEntity<PipelineInstance> {

      return when (pipelineStateManager.pipelineInstance) {
         null -> ResponseEntity.notFound().build()
         else -> ResponseEntity.ok(pipelineStateManager.pipelineInstance)
      }

   }
}

@Component
class PipelineStateManager(val appInfoManager: ApplicationInfoManager, val objectMapper: ObjectMapper, private val pipelineBuilder: PipelineBuilder) {

   var pipelineInstance: PipelineInstance? = null

   fun registerPipeline(pipeline: Pipeline): PipelineInstance {

      BadRequestException.throwIf(pipelineInstance != null, "Runner unavailable. Already running ${pipelineInstance?.spec?.id}")

      // Build the pipeline
      val instance = pipelineBuilder.build(pipeline)

      // Store it
      pipelineInstance = instance

      // Register metadata
      appInfoManager.registerAppMetadata(
         mapOf(
            PIPELINE_METADATA_KEY to objectMapper.writeValueAsString(instance.spec)
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
            throw BadRequestException(message)
         }
      }
   }
}

