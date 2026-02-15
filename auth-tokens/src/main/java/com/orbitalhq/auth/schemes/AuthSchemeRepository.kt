package com.orbitalhq.auth.schemes

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schemas.ServiceName
import reactor.core.publisher.Flux

// Spiritual successor to AuthTokenRepository
// Not very well implemented at the moment.
interface AuthSchemeRepository : AuthSchemeProvider {
   fun saveToken(targetPackage: PackageIdentifier, serviceName: String, token: AuthScheme): SanitizedAuthScheme

   fun listTokensWithoutCredentials(): Map<ServiceName, List<AuthScheme>> {
      return getAllTokens()
         .authenticationTokens.mapValues { (name, schemes) -> schemes.map { it.sanitized() } }
   }

   fun deleteToken(targetPackage: PackageIdentifier, serviceName: String)

   fun getAllTokens(): AuthTokens

   fun listPackages(): List<PackageIdentifier>


   val writeSupported: Boolean
}

// Mainly for testing
object EmptyAuthSchemeRepository : AuthSchemeRepository {
   override fun getAuthSchemes(serviceName: ServiceName): List<AuthScheme> {
      return emptyList()
   }

   override fun getRegisteredKey(presentedKey: String): String? {
      return null
   }

   override fun getAll(): Map<ServiceName, List<AuthScheme>> {
      return emptyMap()
   }

   override val configUpdated: Flux<AuthTokens>
      get() = Flux.empty()

   override fun saveToken(
      targetPackage: PackageIdentifier,
      serviceName: String,
      token: AuthScheme
   ): SanitizedAuthScheme {
      TODO("Not yet implemented")
   }

   override fun deleteToken(targetPackage: PackageIdentifier, serviceName: String) {
      TODO("Not yet implemented")
   }

   override fun getAllTokens(): AuthTokens {
      return AuthTokens.empty()
   }

   override fun listPackages(): List<PackageIdentifier> {
      return emptyList()
   }

   override val writeSupported: Boolean
      get() = false
}
