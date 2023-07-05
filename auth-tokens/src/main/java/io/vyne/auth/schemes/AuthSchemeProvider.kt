package io.vyne.auth.schemes

import io.vyne.schemas.ServiceName

interface AuthSchemeProvider {
   fun getAuthScheme(serviceName: ServiceName): AuthScheme?
}

// for testing
class SimpleAuthSchemeProvider(private val authTokens: AuthTokens) : AuthSchemeProvider {
   override fun getAuthScheme(serviceName: ServiceName): AuthScheme? {
      return authTokens.authenticationTokens[serviceName]
   }

}
