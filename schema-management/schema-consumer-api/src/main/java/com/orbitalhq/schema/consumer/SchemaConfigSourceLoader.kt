package com.orbitalhq.schema.consumer

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.PathGlob
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.config.ConfigSourceLoader
import com.orbitalhq.config.ConfigSourceWriter
import com.orbitalhq.config.FileConfigSourceLoader
import com.orbitalhq.schema.api.SchemaSet
import com.orbitalhq.schemas.Schema
import com.orbitalhq.utils.files.NoOpFileSystemMonitor
import com.typesafe.config.Config
import lang.taxi.packages.SourcesType
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

class SchemaConfigSourceLoader(
   schemaEventSource: SchemaChangedEventProvider,
   /**
    * Generally, this is the name of the config file you want to load (eg., auth.conf).
    * However, can also be a file pattern (eg *.pipeline.json) to support
    * loading multiple files
    */
   private val filePattern: String,
   private val sourceType: SourcesType = "@orbital/config",
) : ConfigSourceLoader, ConfigSourceWriter {

   private val sink = Sinks.many().multicast().directBestEffort<Class<out ConfigSourceLoader>>()

   companion object {
      private val logger = KotlinLogging.logger {}

      object CacheKey
   }

   override val configFileName: String = filePattern

   private val contentCache = ConcurrentHashMap<CacheKey, List<SourcePackage>>()
   private val schemaToAdditionalSourcePaths = ConcurrentHashMap<PackageIdentifier, List<Pair<SourcesType, PathGlob>>>()

   init {
      Flux.from(schemaEventSource.schemaChanged)
         .subscribe { event ->
            val schema = event.newSchemaSet.schema
            load(schema)
            storePackages(event.newSchemaSet)
         }
      if (schemaEventSource is SchemaStore) {
         load(schemaEventSource.schema())
         storePackages(schemaEventSource.schemaSet)
      } else {
         logger.info { "Not loading initial schema, as provided event source is not a schema store.  Will wait for an update event" }
      }
   }

   private var packages: List<SourcePackage> = emptyList()
   private fun storePackages(schemaSet: SchemaSet) {
      this.packages = schemaSet.packages
   }


   private fun load(schema: Schema) {
      // This is a hack, and should find a tidier way.
      // Need to support passing a filename - eg: auth.conf,
      // which should match /a/b/c/auth.conf and auth.conf
      // However, also want to support passing *.conf, which should support /a/b/c/foo.conf and foo.conf
      // So, expanding *.conf to **.conf and auth.conf to **auth.conf.
      // This is a hack, but there's test coverage, so feel free to improve.
      val pathGlob = if (filePattern.startsWith("*")) {
         "glob:*$filePattern"
      } else {
         "glob:**$filePattern"
      }
      val pathMatcher = Paths.get(filePattern).fileSystem.getPathMatcher(pathGlob)
      val filename = Paths.get(filePattern).fileName.toString()
      val sources = schema.additionalSources[sourceType] ?: emptyList()

      // Store the metadata that tells us where our type of config file
      // is written to in each schema.
      // We need this if we have to write back.
      schema.packages.forEach { sourcePackage ->
         val sourcePaths = schema.additionalSourcePaths
            .filter { it.first == sourceType }
         schemaToAdditionalSourcePaths[sourcePackage.identifier] = sourcePaths
      }


      val hoconSources = sources.map { sourcePackage ->
         val requestedSources = sourcePackage.sources
            .filter { pathMatcher.matches(Paths.get(it.name)) }
         sourcePackage.copy(sources = requestedSources)
      }
      val matchedFileNames = hoconSources.flatMap { it.sources }
         .map { it.name }
      logger.info {
         "Loading content from schema with ${schema.packages.size} packages for pattern $pathGlob found ${matchedFileNames.size} matches - ${
            matchedFileNames.joinToString(
               ", "
            )
         }"
      }
      contentCache[CacheKey] = hoconSources
      sink.emitNext(SchemaConfigSourceLoader::class.java, Sinks.EmitFailureHandler.FAIL_FAST)
   }

   override fun saveConfig(targetPackage: PackageIdentifier, updated: Config) {
      val loader = getFileConfigSourceLoader(targetPackage)
      loader.saveConfig(targetPackage, updated)
   }

   /**
    * TO provide writing capabilities for configs loaded from the schema,
    * we look up the transport (and hope that it's file, not git),
    * then wrap a FileConfigSourceLoader that we can defer to
    */
   private fun getFileConfigSourceLoader(targetPackage: PackageIdentifier): FileConfigSourceLoader {
      val sourcesGlobs = schemaToAdditionalSourcePaths[targetPackage] ?: error("No metadata defined for writing sources of type $sourceType to package ${targetPackage.id}. Have additionalSources been configured correctly in the taxi.conf?")
      val sourcePathGlob = when  {
         sourcesGlobs.isEmpty() ->  error("No metadata defined for writing sources of type $sourceType to package ${targetPackage.id}. Have additionalSources been configured correctly in the taxi.conf?")
         sourcesGlobs.size > 1 -> error("Package ${targetPackage.id} lists multiple places to store source files of ${sourceType}. Only a single location is supported when writing")
         else -> sourcesGlobs.single().second
      }
      val targetFile = sourcePathGlob.resolveFileName(filePattern)
      val loader = FileConfigSourceLoader(
         targetFile,
         NoOpFileSystemMonitor,
         targetPackage,
      )
      return loader
   }

   override fun save(targetPackage: PackageIdentifier, source: VersionedSource) {
      val loader = getFileConfigSourceLoader(targetPackage)
      loader.save(targetPackage, source)

   }

   override val packageIdentifiers: List<PackageIdentifier>
      get() {
         return packages.map { it.identifier }
      }

   override fun load(): List<SourcePackage> {
      val loaded = contentCache[CacheKey]
      return if (loaded == null) {
         logger.warn { "The schema has not provided any updates yet." }
         emptyList()
      } else {
         loaded
      }
   }

   override val contentUpdated: Flux<Class<out ConfigSourceLoader>>
      get() = sink.asFlux()
}
