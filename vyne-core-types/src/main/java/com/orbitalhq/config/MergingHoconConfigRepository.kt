package com.orbitalhq.config

import arrow.core.Either
import com.google.common.base.Throwables
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.SourcePackage
import com.typesafe.config.Config
import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import com.typesafe.config.ConfigResolveOptions
import com.typesafe.config.ConfigResolver
import com.typesafe.config.ConfigValue
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.io.File

typealias ConfigSourceErrorMessage = String
/**
 * Models a Config as loaded from a Package,
 * which may or may not contain an error.
 *
 * This does not model an actual resolved config, as resolutions are not applied.
 */
data class ConfigSource<T : Any>(
   val packageIdentifier: PackageIdentifier,
   val unresolvedConfig: Config?,
   /**
    * Note: The typedConfig here has not had resolutions applied.
    * This is because a ConfigSource is parsed standalone, and other sources that provide
    * values may not have been loaded yet.
    */
   val typedConfig: T?,
   val error: ConfigSourceErrorMessage?,
   val configSourceName: String? = null
) {
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
   configRepo: ConfigSourceRepository,
   fallback: Config = ConfigFactory.systemEnvironment()
) : HoconConfigRepository<T>, UpdatableConfigRepository<T>, ConfigSourceWriterProvider by configRepo {
   constructor(
      loaders: List<ConfigSourceLoader>,
      writerProviders: List<ConfigSourceWriterProvider> = emptyList(),
      fallback: Config
   ) : this(ConfigSourceRepository(loaders, writerProviders), fallback)

   abstract fun extract(config: Config): T

   private val loaderTypeName: String = this::class.java.name
   private val configUpdatedSink = Sinks.many().multicast().directBestEffort<T>()

   override val configUpdated: Flux<T> = configUpdatedSink.asFlux()


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

   protected open fun handleConfigUpdated(newConfig: T) {}

   /**
    * A cache of loaded, merged config.
    */
   private val configCache = CacheBuilder.newBuilder()
      .build(object : CacheLoader<CacheKey, T>() {
         override fun load(key: CacheKey): T {
            val loadedSources = configRepo.loadAll()
            val config =  if (loadedSources.isEmpty()) {
               logger.info { "($loaderTypeName) - Loaders returned no config sources, so starting with an empty one." }
               emptyConfig()
            } else {
               _configSources = loadedSources.map { sourcePackage: SourcePackage ->
                  val hoconSource = readRawHoconSource(sourcePackage)
                  try {
                     val configWithFakeResolverValues = readConfig(hoconSource, fallback)
                        .resolve(ConfigResolveOptions.defaults().appendResolver(FakeResolver))
                     val typedConfig = extract(configWithFakeResolverValues)
                     val config = readConfig(hoconSource, fallback)
                     ConfigSource(sourcePackage.identifier, config, typedConfig, null)
                  } catch (e: Exception) {
                     val rootCauseMessage = Throwables.getRootCause(e).message
                     val errorMessage = "Parsing the config from source package ${sourcePackage.packageMetadata.identifier.id} failed: $rootCauseMessage"
                     logger.error(e) { "($loaderTypeName) -  $errorMessage"}
                     val sourceName = if (sourcePackage.sources.size == 1) sourcePackage.sources.first().name else null
                     ConfigSource(sourcePackage.identifier, null, null, errorMessage, sourceName)
                  }
               }
               val healthConfig = _configSources
                  .filter { !it.hasError }
                  .map {
                     it.unresolvedConfig!!
                  }

               if (healthConfig.isEmpty()) {
                  emptyConfig()
               } else {
                  try {
                     val mergedHealthyConfig = healthConfig.reduce { acc, config ->
                        // when merging, "config" values beat "acc" values.
                        config.withFallback(acc)
                     }.resolve() as Config
                     extract(mergedHealthyConfig)
                  } catch (e:Exception) {
                     val errorMessage = Throwables.getRootCause(e).message ?: "A ${e::class.simpleName} exception occurred"
                     if (e is ConfigException) {
                        val matchedSourcePackage = loadedSources.firstOrNull { sourcePackage -> sourcePackage.sources
                           .filter { it.path != null }
                           .any { sourceFile -> sourceFile.path == e.origin().url()?.toURI()?.path } }
                           ?.let { sourcePackage ->
                              _configSources = _configSources + ConfigSource(sourcePackage.identifier, null, null, errorMessage, e.origin().url()?.toURI()?.path)
                              sourcePackage
                           }
                        if (matchedSourcePackage == null) {
                           logger.error { "Could not find a source package for error reported in file ${e.origin().description()} - This error will not be displayed in the UI" }
                        }
                     }
                     logger.error { "Failed to read hocon file: ${e.message}" }
                     emptyConfig()
                  }
               }
            }
            return config
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

   protected fun readRawHoconSource(sourcePackage: SourcePackage): Either<String, File> {
      // This isn't a hard requirement, but it certainly makes life simpler.
      // If this constraint is violated, let's explore the use-case
      require(sourcePackage.sources.size == 1) { "Expected a single source within the source package" }
      val rawConfig = sourcePackage.sources.single().content
      return  if (sourcePackage.sources.single().path != null) {
         Either.Right(File(sourcePackage.sources.single().path!!))
      } else {
         Either.Left(sourcePackage.sources.single().content)
      }
   }

   protected fun unresolvedConfig(rawConfig: Either<String, File>): Config {
      return rawConfig.fold( { configFileContent ->
         ConfigFactory.parseString(configFileContent, ConfigParseOptions.defaults())
            .resolve(ConfigResolveOptions.defaults().setAllowUnresolved(true))
      }, {configFile ->
         ConfigFactory.parseFile(configFile, ConfigParseOptions.defaults())
            .resolve(ConfigResolveOptions.defaults().setAllowUnresolved(true))
      })

   }

   protected open fun readConfig(rawConfig: Either<String, File>, fallback: Config): Config  {
      return rawConfig.fold( { configFileContent ->
         ConfigFactory
            .parseString(configFileContent, ConfigParseOptions.defaults())
            .resolveWith(fallback, ConfigResolveOptions.defaults().setAllowUnresolved(true))
      }, { configFile ->
         ConfigFactory
            .parseFile(configFile, ConfigParseOptions.defaults())
            .resolveWith(fallback, ConfigResolveOptions.defaults().setAllowUnresolved(true))
      })
   }


   init {
      configRepo.contentUpdated.subscribe {
         logger.info { "($loaderTypeName) - Loader ${it.simpleName} indicates sources have changed. Invalidating caches" }
         configCache.invalidateAll()
         configCache.cleanUp()

         // Design choice:  Immediately reload the config, so that we get notified early if the config is invalid.
         val updatedConfig = typedConfig()
         handleConfigUpdated(updatedConfig)
         configUpdatedSink.emitNext(typedConfig(), Sinks.EmitFailureHandler.FAIL_FAST)
      }
      // Design choice:  Immediately reload the config, so that we get notified early if the config is invalid.
      typedConfig()
   }

}

/**
 * Resolver which just returns the requested path as the value,
 * effectively leaving the value unresolved.
 *
 * Used for parsing sources of config, rather than actually building config instances.
 *
 */
private object FakeResolver : ConfigResolver {
   override fun lookup(path: String): ConfigValue {
      val configValue = ConfigFactory.parseMap(mapOf("local" to "\${path}"))
         .getValue("local")
      return configValue
   }

   override fun withFallback(fallback: ConfigResolver): ConfigResolver {
      return fallback
   }

}

data class ConfigRepositoryHealthStatus(
   val errors: List<String>
) {
   val isHealthy: Boolean = errors.isEmpty()
}
