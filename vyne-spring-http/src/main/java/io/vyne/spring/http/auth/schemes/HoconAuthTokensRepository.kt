package io.vyne.spring.http.auth.schemes

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.vyne.PackageIdentifier
import io.vyne.auth.schemes.*
import io.vyne.config.ConfigSourceLoader
import io.vyne.config.ConfigSourceWriter
import io.vyne.config.MergingHoconConfigRepository
import io.vyne.config.getWriter
import io.vyne.schemas.ServiceName
import org.http4k.quoted

/**
 * Reads the AuthTokens from Hocon files.
 * Replaces previous approach, as now support richer config formats (AuthScheme)
 * to allow support for OAuth etc.
 */
class HoconAuthTokensRepository(
   private val loaders: List<ConfigSourceLoader>,
   fallback: Config = ConfigFactory.systemEnvironment(),
) : AuthSchemeProvider, AuthSchemeRepository, MergingHoconConfigRepository<AuthTokens>(loaders, fallback) {

   private val writers = loaders.filterIsInstance<ConfigSourceWriter>()

   override fun extract(config: Config): AuthTokens {
      return AuthTokens.fromConfig(config)
   }

   override fun emptyConfig(): AuthTokens = AuthTokens(emptyMap())

   override fun saveToken(
      targetPackage: PackageIdentifier,
      serviceName: String,
      token: AuthScheme
   ): SanitizedAuthScheme {
      val writer = writers.getWriter(targetPackage)
      val existingValues = loadUnresolvedConfig(writer, targetPackage)

      // Note: calling asHocon(), which defers to the AuthScheme kotlin-specific
      // serialization, (which handles the sealed classes)
      val authTokenHocon = token.asHocon().root()
      val newConfig = ConfigFactory.empty()
         .withValue(authTokenConfigPath(serviceName), authTokenHocon)

      val updated = ConfigFactory.empty()
         .withFallback(newConfig)
         .withFallback(existingValues)

      writer.saveConfig(updated)
      invalidateCache()

      return token.sanitized()
   }

   private fun authTokenConfigPath(serviceName: String): String {
      return "${AuthTokens::authenticationTokens.name}.${serviceName.quoted()}"
   }

   override fun deleteToken(targetPackage: PackageIdentifier, serviceName: String) {
      TODO("Not yet implemented")
   }

   override fun getAllTokens(): AuthTokens {
      return typedConfig()
   }

   override fun listPackages(): List<PackageIdentifier> {
      TODO("Not yet implemented")
   }

   override val writeSupported: Boolean
      get() = TODO("Not yet implemented")

   override fun getAuthScheme(serviceName: ServiceName): AuthScheme? {
      return typedConfig().authenticationTokens[serviceName]
         ?: getWildcardMatch(serviceName)

   }

   private fun getWildcardMatch(serviceName: String): AuthScheme? {
      return getWildcardMatch(serviceName, typedConfig())
   }
}



