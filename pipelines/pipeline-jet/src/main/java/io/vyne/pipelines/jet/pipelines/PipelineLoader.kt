package io.vyne.pipelines.jet.pipelines

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.vyne.pipelines.PipelineSpec
import io.vyne.pipelines.jet.PipelineConfig
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.nio.file.Path

/**
 * Loads pipeline definitions from disk.
 *
 */
@Component
class PipelineLoader(val config: PipelineConfig, val mapper: ObjectMapper, val pipelineManager: PipelineManager) {
   private val logger = KotlinLogging.logger {}

   init {
      if (config.pipelinePath == null) {
         logger.info { "No path defined for loading pipeline specs from." }
      } else {
         loadPipelineSpecs(config.pipelinePath!!)
      }
   }

   private fun loadPipelineSpecs(pipelineSpecPath: Path) {
      pipelineSpecPath
         .toFile()
         .walk()
         .filter { it.name.endsWith(".pipeline.json") }
         .mapNotNull {  file ->
            try {
               val pipelineSpec = mapper.readValue<PipelineSpec<*,*>>(file)
               logger.info { "Read pipelineSpec  ${pipelineSpec.name} from ${file.canonicalPath}" }
               pipelineSpec
            } catch (e:Exception) {
               logger.error { "Failed to read pipeline spec at ${file.canonicalPath}: ${e.message}" }
               null
            }
         }
         .forEach { pipelineSpec ->
            pipelineManager.startPipeline(pipelineSpec)
         }
      TODO("Not yet implemented")
   }
}
