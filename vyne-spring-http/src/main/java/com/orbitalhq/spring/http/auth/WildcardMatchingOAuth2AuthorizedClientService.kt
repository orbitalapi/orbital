package com.orbitalhq.spring.http.auth

import com.orbitalhq.auth.schemes.getServiceNameMatchingOnWildcard
import com.orbitalhq.config.RepositoryWithWildcardSupport
import com.orbitalhq.schemas.ServiceName
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

/**
 * A modified version of InMemoryReactiveOAuth2AuthorizedClientService
 * which will match against clients registered with wildcard expressions.
 *
 * eg: A client registered with id of com.foo.* will match
 * when attempted to load for com.foo.MyService
 */
class WildcardMatchingOAuth2AuthorizedClientService(
   val repository: ReactiveClientRegistrationRepository
) : ReactiveOAuth2AuthorizedClientService {
   private val authorizedClients = ConcurrentHashMap<ServiceName, OAuth2AuthorizedClient>()
   override fun <T : OAuth2AuthorizedClient?> loadAuthorizedClient(
      clientRegistrationId: String,
      principalName: String
   ): Mono<T> {
      return repository.findByRegistrationId(clientRegistrationId)
         .flatMap { registration ->
            Mono.create { sink ->
               val matchedServiceName = when {
                  authorizedClients.containsKey(clientRegistrationId) -> clientRegistrationId
                  else -> getServiceNameMatchingOnWildcard(clientRegistrationId, authorizedClients.keys().toList())
               }
               if (matchedServiceName != null) {
                  val client = authorizedClients[matchedServiceName] as T?
                  if (client != null) {
                     sink.success(client)
                  } else {
                     sink.error(IllegalStateException("Looking up client registration  with key $clientRegistrationId matched $matchedServiceName, but the entry was not found in the map"))
                  }
               } else {
                  sink.success()
               }
            }

         }
   }

   override fun saveAuthorizedClient(
      authorizedClient: OAuth2AuthorizedClient,
      principal: Authentication
   ): Mono<Void> {
      return Mono.fromRunnable {
         // When saving a token, save it with the same id we
         // registered tokens with.
         // ie., tokens can be registered with wildcards, rather than their
         // actual name.
         val key = if (repository is RepositoryWithWildcardSupport) {
            repository.getRegisteredKey(authorizedClient.clientRegistration.clientName)
               ?: authorizedClient.clientRegistration.clientName
         } else {
            authorizedClient.clientRegistration.clientName
         }
         authorizedClients[key] = authorizedClient


      }
   }

   override fun removeAuthorizedClient(clientRegistrationId: String, principalName: String?): Mono<Void> {
      return Mono.fromRunnable { authorizedClients.remove(clientRegistrationId) }
   }

}
