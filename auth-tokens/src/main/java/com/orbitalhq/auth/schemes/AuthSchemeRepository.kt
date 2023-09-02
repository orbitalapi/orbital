package com.orbitalhq.auth.schemes

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schemas.ServiceName

// Spiritual successor to AuthTokenRepository
// Not very well implemented at the moment.
interface AuthSchemeRepository : AuthSchemeProvider {
   fun saveToken(targetPackage: PackageIdentifier, serviceName: String, token: AuthScheme): SanitizedAuthScheme

   fun listTokensWithoutCredentials(): Map<ServiceName, AuthScheme> {
      return getAllTokens()
         .authenticationTokens.mapValues { (name, scheme) -> scheme.sanitized() }
   }

   fun deleteToken(targetPackage: PackageIdentifier, serviceName: String)

   fun getAllTokens(): AuthTokens

   fun listPackages(): List<PackageIdentifier>


   val writeSupported: Boolean
}

// Mainly for testing
object EmptyAuthSchemeRepository : AuthSchemeRepository {
   override fun getAuthScheme(serviceName: ServiceName): AuthScheme? {
      return null
   }

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
