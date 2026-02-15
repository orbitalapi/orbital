package com.orbitalhq.auth.schemes

import arrow.core.filterIsInstance
import com.orbitalhq.config.RepositoryWithWildcardSupport
import com.orbitalhq.schemas.ServiceName
import reactor.core.publisher.Flux

interface AuthSchemeProvider : RepositoryWithWildcardSupport {
   /**
    * Returns all authentication schemes configured for the given service name.
    * Supports wildcard matching (e.g., "com.foo.*").
    */
   fun getAuthSchemes(serviceName: ServiceName): List<AuthScheme>

   /**
    * Returns the first authentication scheme for the given service name.
    * @deprecated Use getAuthSchemes() for multiple auth support
    */
   @Deprecated("Use getAuthSchemes() for multiple auth support", ReplaceWith("getAuthSchemes(serviceName).firstOrNull()"))
   fun getAuthScheme(serviceName: ServiceName): AuthScheme? = getAuthSchemes(serviceName).firstOrNull()

   fun getAll(): Map<ServiceName, List<AuthScheme>>
   val configUpdated: Flux<AuthTokens>
}

inline fun <reified T : AuthScheme> AuthSchemeProvider.getAllOfType(): Map<ServiceName, List<T>> {
   return getAll().mapValues { (_, schemes) ->
      schemes.filterIsInstance<T>()
   }.filterValues { it.isNotEmpty() }
}

// for testing
class SimpleAuthSchemeProvider(private val authTokens: AuthTokens) : AuthSchemeProvider {
   override fun getAuthSchemes(serviceName: ServiceName): List<AuthScheme> {
      return authTokens.authenticationTokens[serviceName] ?: getWildcardMatches(serviceName, authTokens)
   }

   override fun getAll(): Map<ServiceName, List<AuthScheme>> {
      return authTokens.authenticationTokens
   }

   override val configUpdated: Flux<AuthTokens>
      get() = Flux.empty()

   override fun getRegisteredKey(presentedKey: String): String? {
      return getRegisteredKey(presentedKey, authTokens.authenticationTokens)
   }
}

fun getWildcardMatches(serviceName: String, authTokens: AuthTokens): List<AuthScheme> {
   val matchingServiceName = getServiceNameMatchingOnWildcard(serviceName, authTokens.authenticationTokens.keys)
   return matchingServiceName?.let { key -> authTokens.authenticationTokens[key] } ?: emptyList()
}

fun getRegisteredKey(presentedKey: ServiceName, authTokens: Map<ServiceName, List<AuthScheme>>): ServiceName? {
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
