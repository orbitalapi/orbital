package io.vyne.pipelines.jet.pipelines

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import com.typesafe.config.ConfigResolveOptions
import io.vyne.PackageIdentifier
import io.vyne.VersionedSource
import io.vyne.config.*
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import mu.KotlinLogging
import org.apache.commons.io.FilenameUtils
import reactor.core.publisher.Flux
import java.nio.file.Path

/**
 * Loads pipeline definitions from disk.
 *
 */
@Deprecated("Replaced by PipelineConfigRepository")
class PipelineRepository(
   private val loaders: List<ConfigSourceLoader>,
   private val mapper: ObjectMapper,
   private val fallback: Config = ConfigFactory.systemEnvironment()
) {
   constructor(path: Path, mapper: ObjectMapper) : this(
      listOf(FileConfigSourceLoader(path)),
      mapper
   )

   val sourcesChanged: Flux<Class<out ConfigSourceLoader>>

   init {
      val triggers = loaders.map { it.contentUpdated }
      sourcesChanged = Flux.concat(triggers)
   }

   private val logger = KotlinLogging.logger {}

   private val writers: List<ConfigSourceWriter> = loaders.filterIsInstance<ConfigSourceWriter>()

   fun loadPipelines(): List<PipelineSpec<*, *>> {
      val sources = loaders.flatMap { it.load() }
         .flatMap { it.sources }
      return loadPipelines(sources)
   }

   fun loadPipelines(sources: List<VersionedSource>): List<PipelineSpec<*, *>> {
      var failedCount = 0
      val loadedPipelines = sources.mapNotNull { source ->
         try {
            val pipelineSpec = readPipelineFromSource(source) ?: return@mapNotNull null
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

   private fun readPipelineFromSource(source: VersionedSource): PipelineSpec<*, *>? {
      return when (FilenameUtils.getExtension(source.name)) {
         "json" -> mapper.readValue<PipelineSpec<*, *>>(source.content)
         "conf" -> {
            val config = ConfigFactory
               .parseString(source.content, ConfigParseOptions.defaults())
               .resolveWith(fallback, ConfigResolveOptions.defaults())
            val map = config.root().unwrapped()
            mapper.convertValue<PipelineSpec<*, *>>(map)
         }

         else -> {
            logger.warn { "Cannot read pipeline config file ${source.name} as it's not a recognized extension (expected .json or .conf).  Will ignore this one" }
            null
         }
      }
   }

   fun save(packageIdentifier: PackageIdentifier, pipelineSpec: PipelineSpec<*, *>) {
      // Used to write to file system, now defer to a writer to allow us to write a schema source too.
      val writer = writers.firstOrNull { it.packageIdentifier == packageIdentifier }
         ?: error("Unable to find a writer to write to package ${packageIdentifier.id}")
      val pipelineSpecAsMap = mapper.convertValue<Map<String, Any>>(pipelineSpec)
      val config = pipelineSpecAsMap.toConfig()
      val hocon = config.getSafeConfigString()
      val filename = pipelineSpec.id + ".conf"
      val source = VersionedSource(
         filename, packageIdentifier.version, hocon
      )
      writer.save(source)
//      val path = getPipelineFile(pipelineSpec)
//      if (Files.exists(path)) {
//         logger.info { "Overwriting pipeline definition at ${path.toFile().canonicalPath}" }
//      } else {
//         logger.info { "Writing new pipeline definition to ${path.toFile().canonicalPath}" }
//      }
//      mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), pipelineSpec)
   }

   fun deletePipeline(pipelineSpec: PipelineSpec<*, *>) {
      TODO("Not supported whilst we move to using Loaders")
//      val path = getPipelineFile(pipelineSpec)
//      if (Files.exists(path)) {
//         logger.info { "Deleting pipeline definition at ${path.toFile().canonicalPath}" }
//         Files.delete(path)
//      }
   }

}
