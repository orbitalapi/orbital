package com.orbitalhq.auth.schemes

import arrow.core.filterIsInstance
import com.orbitalhq.config.RepositoryWithWildcardSupport
import com.orbitalhq.schemas.ServiceName

interface AuthSchemeProvider : RepositoryWithWildcardSupport {
   fun getAuthScheme(serviceName: ServiceName): AuthScheme?
   fun getAll(): Map<ServiceName, AuthScheme>
}

inline fun <reified T : AuthScheme> AuthSchemeProvider.getAllOfType(): Map<ServiceName, T> {
   return getAll().filterIsInstance()
}

// for testing
class SimpleAuthSchemeProvider(private val authTokens: AuthTokens) : AuthSchemeProvider {
   override fun getAuthScheme(serviceName: ServiceName): AuthScheme? {
      return authTokens.authenticationTokens[serviceName] ?: getWildcardMatch(serviceName, authTokens)
   }

   override fun getAll(): Map<ServiceName, AuthScheme> {
      return authTokens.authenticationTokens
   }

   override fun getRegisteredKey(presentedKey: String): String? {
      return getRegisteredKey(presentedKey, authTokens.authenticationTokens)
   }
}

fun getWildcardMatch(serviceName: String, authTokens: AuthTokens): AuthScheme? {
   val matchingServiceName = getServiceNameMatchingOnWildcard(serviceName, authTokens.authenticationTokens.keys)
   return matchingServiceName?.let { key -> authTokens.authenticationTokens[key] }

}

fun getRegisteredKey(presentedKey: ServiceName, authTokens: Map<ServiceName, AuthScheme>): ServiceName? {
   return if (authTokens[presentedKey] != null) {
      presentedKey
   } else {
      getServiceNameMatchingOnWildcard(presentedKey, authTokens.keys)
   }
}

fun getServiceNameMatchingOnWildcard(serviceName: ServiceName, candidates: Collection<ServiceName>): ServiceName? {
   return candidates
      .asSequence()
      .filter { it.contains("*") }
      .firstOrNull { serviceNameWithWildcard ->
         serviceNameWithWildcard.toRegex().matches(serviceName)
      }
}
