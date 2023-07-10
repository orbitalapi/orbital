package io.vyne.spring.http.auth.schemes

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.vyne.PackageIdentifier
import io.vyne.auth.schemes.*
import io.vyne.config.HoconLoader
import io.vyne.config.HoconWriter
import io.vyne.config.MergingHoconConfigRepository
import io.vyne.schemas.ServiceName
import org.http4k.quoted

/**
 * Reads the AuthTokens from Hocon files.
 * Replaces previous approach, as now support richer config formats (AuthScheme)
 * to allow support for OAuth etc.
 */
class HoconAuthTokensRepository(
   private val loaders: List<HoconLoader>,
   fallback: Config = ConfigFactory.systemEnvironment(),
) : AuthSchemeProvider, AuthSchemeRepository, MergingHoconConfigRepository<AuthTokens>(loaders, fallback) {

   private val writers = loaders.filterIsInstance<HoconWriter>()

   override fun extract(config: Config): AuthTokens {
      return AuthTokens.fromConfig(config)
   }

   override fun emptyConfig(): AuthTokens = AuthTokens(emptyMap())

   override fun saveToken(
      targetPackage: PackageIdentifier,
      serviceName: String,
      token: AuthScheme
   ): SanitizedAuthScheme {
      val writer = writers.firstOrNull { it.packageIdentifier == targetPackage }
         ?: error("No writers found that can write to package ${targetPackage.id}")
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

      return token.sanitized()
   }

   private fun authTokenConfigPath(serviceName: String): String {
      return "${AuthTokens::authenticationTokens.name}.${serviceName.quoted()}"
   }

   private fun loadUnresolvedConfig(writer: HoconWriter, targetPackage: PackageIdentifier): Config {
      val sourcePackages = writer.load()
         .filter { it.identifier == targetPackage }
      // Not a hard requirement, but I need to understand the use case of why this
      // wouldn't be a single value.
      require(sourcePackages.size == 1) { "Expected a single source package, but found ${sourcePackages.size}" }
      val rawSource = readRawHoconSource(sourcePackages.single())
      return unresolvedConfig(rawSource)
   }

   override fun deleteToken(targetPackage: PackageIdentifier, serviceName: String) {
      TODO("Not yet implemented")
   }

   override fun getAllTokens(): AuthTokens {
      TODO("Not yet implemented")
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
      val authTokens = typedConfig()
      val keysWithWildcards = authTokens.authenticationTokens.keys
         .filter { it.contains("*") }
         .asSequence()
         .firstOrNull { tokenServiceNameWildcard -> tokenServiceNameWildcard.toRegex().matches(serviceName) }

      return keysWithWildcards?.let { key -> authTokens.authenticationTokens[key] }

   }


}

