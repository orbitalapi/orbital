package com.orbitalhq.schema.consumer

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.SourcePackage
import com.orbitalhq.config.ConfigSourceWriter
import com.orbitalhq.config.ConfigSourceWriterProvider
import com.orbitalhq.config.FileConfigSourceLoader
import com.orbitalhq.schema.publisher.ProjectLoaderManager
import com.orbitalhq.schema.publisher.loaders.LoaderExposingTaxiProject
import com.orbitalhq.schema.publisher.loaders.SchemaPackageTransport
import lang.taxi.packages.SourcesType
import mu.KotlinLogging
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.nio.file.Path
import java.nio.file.Paths

private object CacheReloadTrigger

private val logger = KotlinLogging.logger { }

/**
 * Wraps a ProjectLoaderManager (which is responsible for loading Taxi projects from places
 * like Git, Filesystems, etc)., and turns it into a ConfigSourceWriter / Loader.
 *
 * Writes are currently supported with editable filesystem projects.
 *
 * This approach is the eventual successor to SchemaConfigSourceLoader.
 */
class ProjectManagerConfigSourceLoader(
   schemaEventSource: SchemaChangedEventProvider,
   private val projectManager: ProjectLoaderManager,
   /**
    * Generally, this is the name of the config file you want to load (eg., auth.conf).
    * However, can also be a file pattern (eg *.pipeline.json) to support
    * loading multiple files
    */
   private val filePattern: String,
   private val sourceType: SourcesType = "@orbital/config"
) : ConfigSourceWriterProvider, BaseCachingConfigLoader(filePattern) {
   private val schemaUpdateFlux: Disposable

   init {
      val initialTrigger = Mono.just(CacheReloadTrigger)
      val schemaChangeTriggers = Flux.from(schemaEventSource.schemaChanged)
         .map { CacheReloadTrigger }

      schemaUpdateFlux = initialTrigger.concatWith(schemaChangeTriggers)
         .flatMap { loadNow() }
         .subscribeOn(Schedulers.boundedElastic())
         .subscribe()
   }

   private fun loadNow(): Mono<List<SourcePackage>> {
      val loaders = projectManager.loaders
      if (loaders.isEmpty()) {
         logger.warn { "No source loaders present (filePattern: $filePattern, sourcesType: $sourceType)" }
         return Mono.empty()
      }
      val listOfObservables: List<Mono<SourcePackage>> = projectManager.loaders
         .map { packageTransport ->
            loadAdditionalSources(packageTransport)
         }
      logger.info { "Found ${listOfObservables.size} source packages to load from ${loaders.size} source loaders (filePattern: $filePattern, sourcesType: $sourceType)" }

      return Flux.fromIterable(listOfObservables)
         .flatMap { it }
         .collectList()
         .map { sourcePackages ->
            buildConfigSourcesCache(sourcePackages)
         }

   }

   /**
    * Loads the source package, and returns a new source package filtered down to the additional sources
    * configured via the provided sourceType
    */
   private fun loadAdditionalSources(packageTransport: SchemaPackageTransport): Mono<SourcePackage> {
      logger.info { "loading additional sources for $packageTransport (filePattern: $filePattern, sourcesType: $sourceType)" }
      return packageTransport.loadNow().map { sourcePackage ->
         SourcePackage(
            sourcePackage.packageMetadata,
            sources = sourcePackage.additionalSources[sourceType] ?: emptyList()
         )
      }.onErrorResume {
         logger.error(it) { "Exception thrown loading additional sources for $packageTransport (filePattern: $filePattern, sourcesType: $sourceType)" }
         Mono.empty()
      }
   }

   override fun getWriter(identifier: PackageIdentifier): ConfigSourceWriter {
      val loader = projectManager.editableLoaders.first { it.packageIdentifier == identifier }
      val sourcePackage = loadAdditionalSources(loader).block()!!
      val writer = when {
         sourcePackage.sources.isEmpty() -> {
            val configFile = createConfigFile(loader, sourcePackage)
            FileConfigSourceLoader(configFile, packageIdentifier = identifier)
         }

         sourcePackage.sources.size == 1 -> {
            FileConfigSourceLoader(Paths.get(sourcePackage.sources.single().name), packageIdentifier = identifier)
         }

         else -> error("Unable to determine the path to write to for $sourceType - found ${sourcePackage.sources.size} sources, couldn't determine which one to write to.")
      }
      return writer
   }

   private fun createConfigFile(loader: SchemaPackageTransport, sourcePackage: SourcePackage): Path {
      require(loader is LoaderExposingTaxiProject) { "Config file does not exist, and cannot create one as the provided loader does not expose a taxi.conf file" }
      return loader.loadTaxiProject()
         .map { (_, taxiConf) ->
            val sourcePatternPath = taxiConf.additionalSources[sourceType]
               ?.let { Paths.get(it) }
            require(sourcePatternPath != null) { "The required config file does not exist, and cannot be created as the taxi.conf file for package ${sourcePackage.identifier.id} does not declare an additionalSources config for type $sourceType." }
            require(taxiConf.packageRootPath != null) { "The taxi.conf has been loaded incorrectly - the packageRootPath is null" }
            val configFilePath = if (sourcePatternPath.fileName.toString().contains("*")) {
               require(!this.filePattern.contains("*")) { "The source directory for additionalSources of type $sourceType in package ${sourcePackage.identifier.id} can not be resolved, as it contains a wildcard - but so does the configured filePattern of $filePattern" }
               taxiConf.packageRootPath!!.resolve(sourcePatternPath.parent).resolve(filePattern)
            } else {
               taxiConf.packageRootPath!!.resolve(sourcePatternPath)
            }
            configFilePath.parent.toFile().mkdirs()
            configFilePath.toFile().createNewFile()
            configFilePath
         }
         .block()!!
   }

   override fun hasWriter(identifier: PackageIdentifier): Boolean {
      return projectManager.editableLoaders.any { it.packageIdentifier == identifier }
   }


}
