package com.orbitalhq.config

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import com.typesafe.config.ConfigResolveOptions
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.SourcePackage
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

/**
 * Models a Config as loaded from a Package,
 * which may contain an error
 */
data class ConfigSource<T : Any>(val packageIdentifier: PackageIdentifier, val config: Config?, val typedConfig: T?, val error:String?) {
   val hasError = error != null
}
/**
 * Takes multiple Config repositories, and merges the result.
 *
 * This allows us to support configs loaded from the file system as part of
 * Orbital config, plus config loaded from schemas as part of the loaded projects.
 *
 * Currently, write operations are not part of the scope of this class,
 * as the merging nature makes this complex.
 */
abstract class MergingHoconConfigRepository<T : Any>(
   loaders: List<ConfigSourceLoader>,
   fallback: Config = ConfigFactory.systemEnvironment()
) : HoconConfigRepository<T> {
   abstract fun extract(config: Config): T

   private val loaderTypeName: String = this::class.java.name
   private val configUpdatedSink = Sinks.many().multicast().directBestEffort<T>()

   val configUpdated = configUpdatedSink.asFlux()

   companion object {
      private object CacheKey

      private val logger = KotlinLogging.logger {}
   }

   override fun typedConfig(): T {
      return configCache[CacheKey]
   }

   val configSources: List<ConfigSource<T>>
      get() {
         typedConfig() // force the cache to be populated
         return _configSources
      }

   protected fun invalidateCache() = configCache.invalidateAll()

   private var _configSources: List<ConfigSource<T>> = emptyList()
   /**
    * A cache of loaded, merged config.
    */
   private val configCache = CacheBuilder.newBuilder()
      .build(object : CacheLoader<CacheKey, T>() {
         override fun load(key: CacheKey): T {
            val loadedSources = loaders.flatMap { it.load() }
               .filter { it.sources.isNotEmpty() }
            return if (loadedSources.isEmpty()) {
               logger.info { "($loaderTypeName) - Loaders returned no config sources, so starting with an empty one." }
               emptyConfig()
            } else {
               _configSources = loadedSources.map { sourcePackage: SourcePackage ->
                  val hoconSource = readRawHoconSource(sourcePackage)
                  try {
                     val config  = readConfig(hoconSource, fallback)
                     val typedConfig = extract(config)
                     ConfigSource(sourcePackage.identifier, config, typedConfig, null)
                  } catch (e: Exception) {
                     logger.error(e) { "($loaderTypeName) - Parsing the config from source package ${sourcePackage.packageMetadata.identifier.id} failed: ${e.message}" }
                     ConfigSource<T>(sourcePackage.identifier, null, null, e.message)
                  }
               }
               val mergedConfig = _configSources
                  .filter { !it.hasError }
                  .map { it.config!! }
                  .reduce { acc, config ->
                  // when merging, "config" values beat "acc" values.
                  config.withFallback(acc)
               }.resolve()
               extract(mergedConfig)
            }
         }
      })

   protected fun loadUnresolvedConfig(writer: ConfigSourceWriter, targetPackage: PackageIdentifier): Config {
      val sourcePackages = writer.load()
         .filter { it.identifier == targetPackage }
      // Not a hard requirement, but I need to understand the use case of why this
      // wouldn't be a single value.
      require(sourcePackages.size == 1) { "Expected a single source package, but found ${sourcePackages.size}" }

      val sourcePackage = sourcePackages.single()
      return if (sourcePackage.sources.isEmpty()) {
         ConfigFactory.empty()
      } else {
         val rawSource = readRawHoconSource(sourcePackage)
         unresolvedConfig(rawSource)
      }

   }

   protected fun readRawHoconSource(sourcePackage: SourcePackage): String {
      // This isn't a hard requirement, but it certainly makes life simpler.
      // If this constraint is violated, let's explore the use-case
      require(sourcePackage.sources.size == 1) { "Expected a single source within the source package" }
      val rawConfig = sourcePackage.sources.single().content
      return rawConfig
   }

   protected fun unresolvedConfig(rawConfig: String): Config {
      return ConfigFactory.parseString(rawConfig, ConfigParseOptions.defaults())
         .resolve(ConfigResolveOptions.defaults().setAllowUnresolved(true))
   }

   protected open fun readConfig(rawConfig: String, fallback: Config): Config =
      ConfigFactory
         .parseString(rawConfig, ConfigParseOptions.defaults())
         .resolveWith(fallback, ConfigResolveOptions.defaults().setAllowUnresolved(true))


   init {
      val allFluxes = loaders.map { it.contentUpdated }
      Flux.merge(allFluxes).subscribe {
         logger.info { "($loaderTypeName) - Loader ${it.simpleName} indicates sources have changed. Invalidating caches" }
         configCache.invalidateAll()
         configCache.cleanUp()
         // Design choice:  Immediately reload the config, so that we get notified early if the config is invalid.
         configUpdatedSink.emitNext(typedConfig(), Sinks.EmitFailureHandler.FAIL_FAST)
      }
      // Design choice:  Immediately reload the config, so that we get notified early if the config is invalid.
      typedConfig()
   }

}
