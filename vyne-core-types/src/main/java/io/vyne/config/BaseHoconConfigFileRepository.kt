package io.vyne.config

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import com.typesafe.config.ConfigRenderOptions
import com.typesafe.config.ConfigResolveOptions
import io.github.config4k.toConfig
import mu.KotlinLogging
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path

private object CacheKey

abstract class BaseHoconConfigFileRepository<T : Any>(
   private val path: Path,
   private val fallback: Config = ConfigFactory.systemProperties()
) {
   private val logger = KotlinLogging.logger {}
   // Urgh.  I hate regex.
   // The actual regex string here is: "\${.*}\"
   // Which is looking for tokens like: "${foo}" (including the quotes).
   // We'll use regex to remove the quotes.
   private val placeholderMarkerRegex = "\"\\\$\\{.*\\}\"".toRegex()

   abstract fun extract(config:Config):T
   abstract fun emptyConfig():T

   /**
    * A cache (to avoid frequently loading from disk on every read)
    * of Config (for writing / mutating) and a TypedConfig (for reading).
    * The Config value returned does not substitue values, making this suitable
    * for updating and persisting back to disk.
    */
   private val configCache = CacheBuilder.newBuilder()
      .build(object : CacheLoader<CacheKey, Pair<Config, T>>() {
         override fun load(key: CacheKey): Pair<Config, T> {
            return if (Files.exists(path)) {
               val configFileContent = path.toFile().readText(Charset.defaultCharset())
               val substitutedRawConfig = ConfigFactory
                  .parseString(configFileContent, ConfigParseOptions.defaults())
                  .resolveWith(fallback, ConfigResolveOptions.defaults().setAllowUnresolved(true))
               val substitutedConfig = extract(substitutedRawConfig)
               val unsubstitutedConfig = ConfigFactory
                  .parseString(configFileContent, ConfigParseOptions.defaults())
                  .resolve(ConfigResolveOptions.defaults().setAllowUnresolved(true))
               unsubstitutedConfig to substitutedConfig
            } else {
               logger.info { "No config file exists at ${path.toFile().canonicalPath}, starting with an empty one" }
               val emptyConfigAsConfig: Config = emptyConfig().toConfig()
               val emptyTypedConfig = emptyConfig()
               emptyConfigAsConfig to emptyTypedConfig
            }
         }
      })


   protected fun typedConfig(): T {
      return configCache.get(CacheKey).second
   }

   protected fun unresolvedConfig(): Config {
      return configCache.get(CacheKey).first
   }


   protected open fun saveConfig(config: Config) {
      val updatedConfigString = config.root()
         .render(
            ConfigRenderOptions.defaults()
               .setFormatted(true)
               .setComments(true)
               .setOriginComments(false)
               .setJson(false)
         )
      // Unfortunately, the HOCON library is designed for reading,
      // but kinda shitty at writing.
      // There doesn't appear to be a way to get the config
      // output with placeholders in tact, they're always escaped.
      // So, here we replace "${foo}" with ${foo}.
      val configWithPlaceholderQuotesRemoved = removeQuotesFromPlaceholderMarkers(updatedConfigString)

      path.toFile().writeText(configWithPlaceholderQuotesRemoved)
      configCache.invalidateAll()
   }

   private fun removeQuotesFromPlaceholderMarkers(updatedConfString: String): String {
      return updatedConfString.replace(placeholderMarkerRegex) {
         it.value.removeSurrounding("\"")
      }
   }
}

fun Any.toConfig(): Config {
   return this.toConfig("root")
      .getConfig("root")
      .root()
      .toConfig()
}
