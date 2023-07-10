package io.vyne.config

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import com.typesafe.config.ConfigResolveOptions
import io.vyne.SourcePackage
import mu.KotlinLogging
import reactor.core.publisher.Flux


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
   loaders: List<HoconLoader>,
   fallback: Config = ConfigFactory.systemEnvironment()
) : HoconConfigRepository<T> {
   abstract fun extract(config: Config): T

   companion object {
      private object CacheKey

      private val logger = KotlinLogging.logger {}
   }

   override fun typedConfig(): T {
      return configCache[CacheKey]
   }

   /**
    * A cache of loaded, merged config.
    */
   private val configCache = CacheBuilder.newBuilder()
      .build(object : CacheLoader<CacheKey, T>() {
         override fun load(key: CacheKey): T {
            val loadedSources = loaders.flatMap { it.load() }
            return if (loadedSources.isEmpty()) {
               logger.info { "(${this::class.simpleName}) - Loaders returned no config sources, so starting with an empty one." }
               emptyConfig()
            } else {
               logger.info { "(${this::class.simpleName}) - Loading config files returned ${loadedSources.size} sources" }
               val loadedConfigs = loadedSources.map { sourcePackage: SourcePackage ->
                  val rawConfig = readRawHoconSource(sourcePackage)
                  try {
                     readConfig(rawConfig, fallback)
                  } catch (e: Exception) {
                     logger.error(e) { "(${this::class.simpleName}) - Parsing the config from source package ${sourcePackage.packageMetadata.identifier.id} failed: ${e.message}" }
                     throw e
                  }

               }
               val mergedConfig = mergeConfigs(loadedConfigs)
               extract(mergedConfig)
            }
         }
      })

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

   protected fun readConfig(rawConfig: String, fallback: Config): Config =
      ConfigFactory
         .parseString(rawConfig, ConfigParseOptions.defaults())
         .resolveWith(fallback, ConfigResolveOptions.defaults().setAllowUnresolved(true))

   private fun mergeConfigs(loadedConfigs: List<Config>): Config {
      return loadedConfigs.reduce { acc, config -> acc.withFallback(config) }
   }

   init {
      val allFluxes = loaders.map { it.contentUpdated }
      Flux.merge(allFluxes).subscribe {
         logger.info { "(${this::class.simpleName}) - Loader ${it.simpleName} indicates sources have changed. Invalidating caches" }
         configCache.invalidateAll()
         configCache.cleanUp()
         // Design choice:  Immediately reload the config, so that we get notified early if the config is invalid.
         typedConfig()
      }
      // Design choice:  Immediately reload the config, so that we get notified early if the config is invalid.
      typedConfig()

   }
}