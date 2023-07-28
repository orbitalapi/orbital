package io.vyne.auth.schemes

import io.vyne.schemas.ServiceName

interface AuthSchemeProvider {
   fun getAuthScheme(serviceName: ServiceName): AuthScheme?
}

// for testing
class SimpleAuthSchemeProvider(private val authTokens: AuthTokens) : AuthSchemeProvider {
   override fun getAuthScheme(serviceName: ServiceName): AuthScheme? {
      return authTokens.authenticationTokens[serviceName] ?: getWildcardMatch(serviceName, authTokens)
   }

}

fun getWildcardMatch(serviceName: String, authTokens: AuthTokens): AuthScheme? {
   val keysWithWildcards = authTokens.authenticationTokens.keys
      .filter { it.contains("*") }
      .asSequence()
      .firstOrNull { tokenServiceNameWildcard -> tokenServiceNameWildcard.toRegex().matches(serviceName) }

   return keysWithWildcards?.let { key -> authTokens.authenticationTokens[key] }

}
