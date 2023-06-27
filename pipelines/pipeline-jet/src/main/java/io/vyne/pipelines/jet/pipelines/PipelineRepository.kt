package io.vyne.pipelines.jet.pipelines

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.vyne.VersionedSource
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import mu.KotlinLogging
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

   fun loadPipelines(sources: List<VersionedSource>): List<PipelineSpec<*, *>> {
      var failedCount = 0
      val loadedPipelines = sources.mapNotNull { source ->
         try {
            val pipelineSpec = mapper.readValue<PipelineSpec<*, *>>(source.content)
            logger.info { "Read pipeline spec \"${pipelineSpec.name}\" from ${source.name}." }
            pipelineSpec
         } catch (e: Exception) {
            logger.error { "Failed to read pipeline spec at ${source.name}: ${e.message}." }
            failedCount++
            null
         }
      }
      val duplicateKeys = loadedPipelines.groupingBy { it.id }.eachCount().filter { it.value > 1 }.keys
      if (duplicateKeys.isNotEmpty()) {
         throw IllegalStateException("Found duplicate pipeline ids: ${duplicateKeys.joinToString(", ")}. Please make sure that each pipeline has a unique id.")
      }

      logger.info { "Loaded ${loadedPipelines.size} pipelines, with $failedCount failed to load." }
      return loadedPipelines
   }

   private fun loadPipelineSpecs(pipelineSpecPath: Path): List<PipelineSpec<*, *>> {
      val sources = pipelineSpecPath
         .toFile()
         .walk()
         .filter { it.name.endsWith(".pipeline.json") }
         .mapNotNull { file ->
            try {
               VersionedSource(
                  name = file.canonicalPath,
                  version = VersionedSource.DEFAULT_VERSION.toString(),
                  file.readText()
               )
            } catch (e: Exception) {
               logger.error { "Failed to read pipeline spec at ${file.canonicalPath}: ${e.message}." }
               null
            }
         }.toList()
      return loadPipelines(sources)
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

   fun deletePipeline(pipelineSpec: PipelineSpec<*, *>) {
      val path = getPipelineFile(pipelineSpec)
      if (Files.exists(path)) {
         logger.info { "Deleting pipeline definition at ${path.toFile().canonicalPath}" }
         Files.delete(path)
      }
   }

   private fun getPipelineFile(pipelineSpec: PipelineSpec<*, *>): Path {
      return pipelinePath.resolve(pipelineSpec.id + ".pipeline.json")
   }
}
