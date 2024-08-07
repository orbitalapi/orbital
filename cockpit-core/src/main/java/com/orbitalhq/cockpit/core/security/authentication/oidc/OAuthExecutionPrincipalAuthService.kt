package com.orbitalhq.cockpit.core.security.authentication.oidc

import com.orbitalhq.auth.authentication.ExecutionPrincipalAuthenticationService
import com.orbitalhq.auth.authentication.VyneUser
import com.orbitalhq.auth.authentication.toVyneUser
import mu.KotlinLogging
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.security.Principal

/**
 * Loads a token for Oribtal's execution principal from
 * a configured OAuth IDP
 */
class OAuthExecutionPrincipalAuthService(
   private val webClient: WebClient,
   private val clientId: String,
   private val clientSecret: String,
   private val tokenUri: String,
   private val authenticationManager: ReactiveAuthenticationManager
) : ExecutionPrincipalAuthenticationService {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   override fun loadPrincipal(): Mono<Principal> {
      return fetchToken()
         .flatMap { token ->
            authenticationManager.authenticate(BearerTokenAuthenticationToken(token))
               .doOnError { e ->
                  logger.error(e) { "Failed to authenticate token for execution principal" }
               }
         }
   }

   override fun loadUser(): Mono<VyneUser> {
      return loadPrincipal().map { principal ->
         (principal as Authentication).toVyneUser()
      }
   }

   private fun fetchToken(): Mono<String> {
      logger.info { "Attempting to fetch authentication token for execution principal" }
      return webClient.post()
         .uri(tokenUri)
         .header("Content-Type").header("Content-Type", "application/x-www-form-urlencoded")
         .bodyValue("grant_type=client_credentials&client_id=$clientId&client_secret=$clientSecret")
         .retrieve()
         .bodyToMono(Map::class.java)
         .map { it["access_token"] as String }
         .doOnNext {
            logger.info { "Successfully fetching authentication token for execution principal" }
         }
         .doOnError { e ->
            logger.error(e) { "Failed to retrieve authentication token for execution principal - ${e.message}" }
         }
   }
}
