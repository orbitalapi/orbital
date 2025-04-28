package com.orbitalhq.spring.http.auth.schemes

import com.google.common.cache.CacheBuilder
import com.orbitalhq.auth.schemes.AuthSchemeProvider
import com.orbitalhq.auth.schemes.OAuth2
import com.orbitalhq.config.RepositoryWithWildcardSupport
import com.orbitalhq.schemas.ServiceName
import mu.KotlinLogging
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

/**
 * Decorates our HOCON config loader approach for loading Auth Tokens,
 * and exposes it as a Spring ReactiveClientRegistrationRepository
 */
class HoconOAuthClientRegistrationRepository(
   private val authSchemeProvider: AuthSchemeProvider
) : ReactiveClientRegistrationRepository, RepositoryWithWildcardSupport {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   init {

      authSchemeProvider.configUpdated.subscribe {
         logger.info { "authorisation config updated - invalidating cache" }
         cache.invalidateAll()
      }
   }

   private val cache = CacheBuilder.newBuilder()
      .maximumSize(1)
      .build<Int, ConcurrentHashMap<String, ClientRegistration>>()

   override fun findByRegistrationId(registrationId: String): Mono<ClientRegistration> {
      return Mono.create { sink ->
         try {
            val currentTokensHash = authSchemeProvider.hashCode()
            val clientRegistrations = cache.get(currentTokensHash) { ConcurrentHashMap() }
            val clientRegistration = clientRegistrations.getOrPut(registrationId) {
               val authScheme = authSchemeProvider.getAuthScheme(registrationId)
                  ?: error("No Auth defined for registration $registrationId")
               require(authScheme is OAuth2) { "Expected auth scheme $registrationId to be ${OAuth2::class.simpleName} but was ${authScheme::class.simpleName}" }
               val registration = authScheme.asClientRegistration(registrationId)
               logger.debug { "Created OAuth registration from registered token $registrationId" }
               registration
            }
            sink.success(clientRegistration)
         } catch (e: Exception) {
            sink.error(e)
         }
      }
   }

   override fun getRegisteredKey(presentedKey: ServiceName): ServiceName? {
      return authSchemeProvider.getRegisteredKey(presentedKey)
   }
}


