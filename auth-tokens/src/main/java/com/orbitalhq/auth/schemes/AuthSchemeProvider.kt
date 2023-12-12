package com.orbitalhq.auth.schemes

import arrow.core.filterIsInstance
import com.orbitalhq.schemas.ServiceName

interface AuthSchemeProvider {
   fun getAuthScheme(serviceName: ServiceName): AuthScheme?
   fun getAll():Map<ServiceName,AuthScheme>
}

inline fun <reified T : AuthScheme> AuthSchemeProvider.getAllOfType():Map<ServiceName,T> {
   return getAll().filterIsInstance()
}

// for testing
class SimpleAuthSchemeProvider(private val authTokens: AuthTokens) : AuthSchemeProvider {
   override fun getAuthScheme(serviceName: ServiceName): AuthScheme? {
      return authTokens.authenticationTokens[serviceName] ?: getWildcardMatch(serviceName, authTokens)
   }

   override fun getAll(): Map<ServiceName,AuthScheme> {
      return authTokens.authenticationTokens
   }
}

fun getWildcardMatch(serviceName: String, authTokens: AuthTokens): AuthScheme? {
   val keysWithWildcards = authTokens.authenticationTokens.keys
      .filter { it.contains("*") }
      .asSequence()
      .firstOrNull { tokenServiceNameWildcard -> tokenServiceNameWildcard.toRegex().matches(serviceName) }

   return keysWithWildcards?.let { key -> authTokens.authenticationTokens[key] }

}
