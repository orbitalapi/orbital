package com.orbitalhq.spring.http.auth

import com.orbitalhq.auth.schemes.AuthSchemeProvider
import com.orbitalhq.auth.schemes.AuthTokens
import com.orbitalhq.auth.schemes.OAuth2
import com.orbitalhq.auth.schemes.getAllOfType
import com.orbitalhq.config.UpdatableConfigRepository
import com.orbitalhq.spring.http.auth.schemes.createAuthorizedClient
import mu.KotlinLogging
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService
import reactor.core.Disposable
import java.time.Instant

/**
 * When the underlying config changes (typcially in a HoconAuthTokensRepository),
 * this class is responsible for updating the ReactiveOAuth2AuthorizedClientService
 * to add expired tokens, forcing refresh on next call.
 */
class OAuthRefreshTokenManager(
   private val authorizedClientService: ReactiveOAuth2AuthorizedClientService,
   private val authSchemeProvider: AuthSchemeProvider
) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }
   init {
      // Init logic moved to start().
      // Otherwise, in places like StandaloneVyneFactory, we end up with leaks
   }

   /**
    * Listens for updates form the authSchemeProvider (if supported)
    * and refreshes tokens
    */
   fun resetTokensOnUpdates(resetNow: Boolean = true): Disposable? {
      val disposable = if (authSchemeProvider is UpdatableConfigRepository<*>) {
         (authSchemeProvider as UpdatableConfigRepository<AuthTokens>)
            .configUpdated
            .subscribe { resetRefreshTokens() }
      } else null
      if (resetNow) {
         resetRefreshTokens()
      }

      return disposable
   }

   fun resetRefreshTokens() {
      authSchemeProvider.getAllOfType<OAuth2>()
         .filter { (serviceName, token) -> token.refreshToken != null }
         .ifEmpty {
            logger.info { "Auth config has changed. No OAuth refresh tokens configured, so nothing to do" }
            emptyMap()
         }
         .forEach { (serviceName, oauthToken) ->
            logger.info { "Resetting auth for $serviceName to expired access token, forcing refresh on next call" }
            val (principal, client) = oauthToken.createAuthorizedClient(
               serviceName, "expiredToken", Instant.MIN,
               Instant.MIN.plusSeconds(60)
            )
            authorizedClientService.saveAuthorizedClient(
               client, principal
            ).subscribe()
            logger.info { "Auth for $serviceName has been reset" }
         }
   }
}
