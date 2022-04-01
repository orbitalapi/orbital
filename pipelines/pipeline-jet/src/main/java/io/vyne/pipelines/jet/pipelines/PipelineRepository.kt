package io.vyne.pipelines.jet.pipelines

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

/**
 * Loads pipeline definitions from disk.
 *
 */
class PipelineRepository(val pipelinePath: Path, val mapper: ObjectMapper) {
   private val logger = KotlinLogging.logger {}

   fun loadPipelines(): List<PipelineSpec<*, *>> {
      return loadPipelineSpecs(pipelinePath)
   }

   private fun loadPipelineSpecs(pipelineSpecPath: Path): List<PipelineSpec<*, *>> {
      var failedCount = 0
      val loadedPipelines = pipelineSpecPath
         .toFile()
         .walk()
         .filter { it.name.endsWith(".pipeline.json") }
         .mapNotNull { file ->
            try {
               val pipelineSpec = mapper.readValue<PipelineSpec<*, *>>(file)
               logger.info { "Read pipelineSpec  ${pipelineSpec.name} from ${file.canonicalPath}" }
               pipelineSpec
            } catch (e: Exception) {
               logger.error { "Failed to read pipeline spec at ${file.canonicalPath}: ${e.message}" }
               failedCount++
               null
            }
         }.toList()

      logger.info { "Loaded  ${loadedPipelines.size} pipelines, with $failedCount failed to load" }
      return loadedPipelines
   }

   fun save(pipelineSpec: PipelineSpec<*, *>) {
      val path = getPipelineFile(pipelineSpec)
      if (Files.exists(path)) {
         logger.info { "Overwriting pipeline definition at ${path.toFile().canonicalPath}" }
      } else {
         logger.info { "Writing new pipeline definition to ${path.toFile().canonicalPath}" }
      }
      mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), pipelineSpec)
   }

   private fun getPipelineFile(pipelineSpec: PipelineSpec<*, *>): Path {
      return pipelinePath.resolve(pipelineSpec.id + ".pipeline.json")
   }
}
