package io.vyne.spring.http.auth

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import com.typesafe.config.ConfigRenderOptions
import com.typesafe.config.ConfigResolveOptions
import io.github.config4k.extract
import io.github.config4k.toConfig
import mu.KotlinLogging
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path

private object CacheKey

/**
 * An auth token repository which uses config files to read/write
 * auth tokens to/from.
 *
 * Files are stored in HOCON format (https://github.com/lightbend/config#examples-of-hocon)
 * which allows for variable substitution from the env.
 *
 * If the file does not exist, it will be created when a token
 * is saved.
 *
 * This allows users to store sensitive values either directly in the file,
 * or injected from the env.
 */
class ConfigFileAuthTokenRepository(
   private val path: Path,
   private val fallback: Config = ConfigFactory.systemProperties()
) : AuthTokenRepository {
   private val logger = KotlinLogging.logger {}

   // Urgh.  I hate regex.
   // The actual regex string here is: "\${.*}\"
   // Which is looking for tokens like: "${foo}" (including the quotes).
   // We'll use regex to remove the quotes.
   private val placeholderMarkerRegex = "\"\\\$\\{.*\\}\"".toRegex()

   override val writeSupported: Boolean = true

   /**
    * A cache (to avoid frequently loading from disk on every read)
    * of Config (for writing / mutating) and AuthConfig (for reading).
    * The Config value returned does not substitue values, making this suitable
    * for updating and persisting back to disk.
    */
   private val authConfigCache = CacheBuilder.newBuilder()
      .build(object : CacheLoader<CacheKey, Pair<Config, AuthConfig>>() {
         override fun load(key: CacheKey): Pair<Config, AuthConfig> {
            return if (Files.exists(path)) {
               val configFileContent = path.toFile().readText(Charset.defaultCharset())
               val substitutedConfig = ConfigFactory
                  .parseString(configFileContent, ConfigParseOptions.defaults())
                  .resolveWith(fallback, ConfigResolveOptions.defaults().setAllowUnresolved(true))
                  .extract<AuthConfig>()
               val unsubstitutedConfig = ConfigFactory
                  .parseString(configFileContent, ConfigParseOptions.defaults())
                  .resolve(ConfigResolveOptions.defaults().setAllowUnresolved(true))
               unsubstitutedConfig to substitutedConfig
            } else {
               logger.info { "No auth config file exists at $path, starting with an empty one" }
               val emptyConfig = AuthConfig().toConfig()
               emptyConfig to AuthConfig()
            }
         }
      })

   private fun resolvedConfig(): AuthConfig {
      return authConfigCache.get(CacheKey).second
   }

   private fun unresolvedConfig(): Config {
      return authConfigCache.get(CacheKey).first
   }

   override fun getToken(serviceName: String): AuthToken? {
      val config = resolvedConfig()
      return config.authenticationTokens[serviceName]
   }

   override fun saveToken(serviceName: String, token: AuthToken) {
      val newConfig = AuthConfig(mutableMapOf(serviceName to token))
         .toConfig()
      // Use the existing unresolvedConfig to ensure that when we're
      // writing back out, that tokens that have been resolved
      // aren't accidentally written with their real values back out
      val existingValues = unresolvedConfig()

      val updated = ConfigFactory.empty()
         .withFallback(newConfig)
         .withFallback(existingValues)

      val updatedConfString = updated.root()
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
      val configWithPlaceholderQuotesRemoved = removeQuotesFromPlaceholderMarkers(updatedConfString)

      Files.writeString(path, configWithPlaceholderQuotesRemoved)
      authConfigCache.invalidateAll()
   }

   private fun removeQuotesFromPlaceholderMarkers(updatedConfString: String): String {
      return updatedConfString.replace(placeholderMarkerRegex) {
         it.value.removeSurrounding("\"")
      }
   }

}

private fun Any.toConfig(): Config {
   return this.toConfig("root")
      .getConfig("root")
      .root()
      .toConfig()
}
