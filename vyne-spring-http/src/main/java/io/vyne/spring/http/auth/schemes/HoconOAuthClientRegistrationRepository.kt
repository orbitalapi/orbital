package io.vyne.spring.http.auth.schemes

import com.google.common.cache.CacheBuilder
import io.vyne.auth.schemes.AuthSchemeProvider
import io.vyne.auth.schemes.OAuth2
import mu.KotlinLogging
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

/**
 * Decorates our HOCON config loader approach for loading Auth Tokens,
 * and exposes it as a Spring ReactiveClientRegistrationRepository
 */
class HoconOAuthClientRegistrationRepository(
   private val authSchemeProvider: AuthSchemeProvider
) : ReactiveClientRegistrationRepository {

   companion object {
      private val logger = KotlinLogging.logger {}
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
               val registration = ClientRegistration.withRegistrationId(registrationId)
                  .clientId(authScheme.clientId)
                  .clientSecret(authScheme.clientSecret)
                  .tokenUri(authScheme.accessTokenUrl)
                  .clientAuthenticationMethod(authScheme.method.asSpringAuthMethod())
                  .authorizationGrantType(authScheme.grantType.asSpringGrantType())
                  .build()
               logger.debug { "Created OAuth registration from registered token $registrationId" }
               registration
            }
            sink.success(clientRegistration)
         } catch (e: Exception) {
            sink.error(e)
         }
      }
   }
}


private fun OAuth2.AuthorizationGrantType.asSpringGrantType(): AuthorizationGrantType {
   return when (this) {
      OAuth2.AuthorizationGrantType.ClientCredentials -> AuthorizationGrantType.CLIENT_CREDENTIALS
      OAuth2.AuthorizationGrantType.RefreshToken -> AuthorizationGrantType.REFRESH_TOKEN
      OAuth2.AuthorizationGrantType.AuthorizationCode -> AuthorizationGrantType.AUTHORIZATION_CODE
   }

}

private fun OAuth2.AuthenticationMethod.asSpringAuthMethod(): ClientAuthenticationMethod {
   return when (this) {
      OAuth2.AuthenticationMethod.Basic -> ClientAuthenticationMethod.CLIENT_SECRET_BASIC
      OAuth2.AuthenticationMethod.JWT -> ClientAuthenticationMethod.CLIENT_SECRET_JWT
      OAuth2.AuthenticationMethod.Post -> ClientAuthenticationMethod.CLIENT_SECRET_POST
   }

}
