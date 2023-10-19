package com.orbitalhq.pipelines.jet.pipelines

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.VersionedSource
import com.orbitalhq.config.*
import com.orbitalhq.pipelines.jet.PipelineJsonConfig
import com.orbitalhq.pipelines.jet.api.transport.PipelineSpec

/**
 * Design choice:
 * We're persisting pipelines as hocon files, to be consistent
 * with other config, and to support variable resolution etc.
 *
 * choosing to treat each file as a list of pipelines, even
 * though we typically only write one per file.
 *
 * This makes merging easier, and also helps us
 * disambiguate things like pipeline specs from other files
 * such as env variables.
 */
data class PipelineSpecList(
   val pipelines: List<PipelineSpec<*, *>> = emptyList()
)

class PipelineConfigRepository(
   loaders: List<ConfigSourceLoader>,
   fallback: Config = ConfigFactory.systemEnvironment(),

   ) : MergingHoconConfigRepository<List<PipelineSpec<*, *>>>(loaders, fallback) {

   private val mapper: ObjectMapper = PipelineJsonConfig.lenientReadingMapper()
   private val writers: List<ConfigSourceWriter> = loaders.filterIsInstance<ConfigSourceWriter>()

   init {
      // The cache has already been primed with an empty
      // config, because we called typedConfig() during the superclass
      // init {} method.
      invalidateCache()
      typedConfig()
   }

   override fun extract(config: Config): List<PipelineSpec<*, *>> {
      // Ignore the compiler warning --
      // This happens during the base class calling init { },
      // before the default assignments have been evaluated.
      if (mapper == null) {
         return emptyList()
      }

      val configAsMap = config.root().unwrapped()
      val pipelineSpecList = mapper.convertValue(configAsMap, PipelineSpecList::class.java)

      return pipelineSpecList.pipelines
   }

   override fun emptyConfig(): List<PipelineSpec<*, *>> {
      return emptyList()
   }

   fun loadPipelines(): List<PipelineSpec<*, *>> {
      return typedConfig()
   }

   fun save(packageIdentifier: PackageIdentifier, pipelineSpec: PipelineSpec<*, *>) {
      // Used to write to file system, now defer to a writer to allow us to write a schema source too.
      val writer = writers.firstOrNull { it.packageIdentifiers.contains(packageIdentifier) }
         ?: error("Unable to find a writer to write to package ${packageIdentifier.id}")
      val pipelineList = PipelineSpecList(listOf(pipelineSpec))
      val pipelineSpecAsMap = mapper.convertValue<Map<String, Any>>(pipelineList)
      val config = pipelineSpecAsMap.toHocon()
      val hocon = config.getSafeConfigString()
      val filename = pipelineSpec.id + ".conf"
      val source = VersionedSource(
         filename, packageIdentifier.version, hocon
      )
      writer.save(packageIdentifier, source)
   }

   fun deletePipeline(packageIdentifier: PackageIdentifier, pipelineSpec: PipelineSpec<*, *>) {
      TODO("Not supported whilst we move to using Loaders")
//      val path = getPipelineFile(pipelineSpec)
//      if (Files.exists(path)) {
//         logger.info { "Deleting pipeline definition at ${path.toFile().canonicalPath}" }
//         Files.delete(path)
//      }
   }
}
