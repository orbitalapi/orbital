package io.vyne.spring.http.auth

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.vyne.config.BaseHoconConfigFileRepository
import io.vyne.config.toConfig
import mu.KotlinLogging
import org.http4k.quoted
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
   path: Path,
   fallback: Config = ConfigFactory.systemProperties()
) : AuthTokenRepository, BaseHoconConfigFileRepository<AuthConfig>(path, fallback) {
   private val logger = KotlinLogging.logger {}


   override val writeSupported: Boolean = true

   override fun emptyConfig(): AuthConfig {
      return AuthConfig()
   }

   override fun extract(config: Config): AuthConfig = config.extract()


   override fun getToken(serviceName: String): AuthToken? {
      val config = typedConfig()
      return config.authenticationTokens[serviceName]
   }

   override fun listTokens(): List<NoCredentialsAuthToken> {
      return this.typedConfig()
         .authenticationTokens.map { (serviceName, token) ->
            NoCredentialsAuthToken(serviceName, token.tokenType)
         }
   }

   private fun authTokenConfigPath(serviceName: String): String {
      return "authenticationTokens.${serviceName.quoted()}"
   }


   override fun deleteToken(serviceName: String) {
      saveConfig(
         unresolvedConfig()
            .withoutPath(authTokenConfigPath(serviceName))
      )
   }

   override fun saveToken(serviceName: String, token: AuthToken) {
//      val newConfig = AuthConfig(mutableMapOf(serviceName to token))
//         .toConfig()
      val newConfig = ConfigFactory.empty()
         .withValue(authTokenConfigPath(serviceName), token.toConfig().root())

      // Use the existing unresolvedConfig to ensure that when we're
      // writing back out, that tokens that have been resolved
      // aren't accidentally written with their real values back out
      val existingValues = unresolvedConfig()

      val updated = ConfigFactory.empty()
         .withFallback(newConfig)
         .withFallback(existingValues)

      saveConfig(updated)
   }

}
