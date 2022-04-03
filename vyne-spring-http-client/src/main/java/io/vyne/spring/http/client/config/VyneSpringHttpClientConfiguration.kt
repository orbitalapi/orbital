package io.vyne.spring.http.client.config

import io.vyne.spring.http.client.VyneHttpClient
import io.vyne.spring.http.client.secure.OAuthClientCredentialsRestTemplateInterceptor
import mu.KotlinLogging
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.web.client.RestTemplate

private val logger = KotlinLogging.logger {  }
@Configuration
class VyneSpringHttpClientConfiguration {
   @Bean
   fun vyneHttpClient(vyneHttpClientInfo: VyneHttpClientConfig): VyneHttpClient {
      val restTemplate = RestTemplate()
      val restTemplateBuilder = RestTemplateBuilder()
      return if (vyneHttpClientInfo.isSecure) {
         logger.info { "Registering a secure vyne http client with $vyneHttpClientInfo" }
         VyneHttpClient(registerSecureHttpClient(restTemplateBuilder, vyneHttpClientInfo), vyneHttpClientInfo.vyneUrl)
      } else {
         logger.info { "Registering an unsecure vyne http client with $vyneHttpClientInfo" }
         VyneHttpClient(restTemplate, vyneHttpClientInfo.vyneUrl)
      }
   }

   /**
    * Returns a RestTemplate instance with an interceptor to provide Bearer Authorization header value by interacting
    * with Open Idp server by performing the required actions to implement Oauth2.0 Client Credentials grant
    * see https://www.oauth.com/oauth2-servers/access-tokens/client-credentials/ for details.
    *
    * The RestTemplate returned by this class will authenticate the request by passing @see VyneHttpClientConfig::clientId
    * and clientSecret to authentication server VyneHttpClientConfig::clientTokenUri as part of client credentials request.
    *
    * i.e.
    *
    * POST /token HTTP/1.1
    * Host: VyneHttpClientConfig::clientTokenUri
    * grant_type=client_credentials
    *  &client_id=VyneHttpClientConfig::clientId
    *  &client_secret=VyneHttpClientConfig::clientSecret
    *
    * For the above request, Authentication server will return:
    *
    * HTTP/1.1 200 OK
    *  Content-Type: application/json
    * Cache-Control: no-store
    * {
    * "access_token":"MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3",
    * "token_type":"Bearer",
    *  "expires_in":3600,
    * "refresh_token":"IwOGYzYTlmM2YxOTQ5MGE3YmNmMDFkNTVk",
    * "scope":"create"
    * }
    *
    * the RestTemplate will set the Authorization header value to 'Bearer access_token' for the outgoing request.
    */
   private fun registerSecureHttpClient(restTemplateBuilder: RestTemplateBuilder, info: VyneHttpClientConfig): RestTemplate {
      val vyneClientRegistrationId = "vyne-http-client"
      val clientRegistration = ClientRegistration
         .withRegistrationId(vyneClientRegistrationId)
         .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
         .clientId(info.id)
         .clientSecret(info.secret)
         .tokenUri(info.tokenUri)
         .build()

      val clientRegistrationRepository = InMemoryClientRegistrationRepository(listOf(clientRegistration))
      val oAuth2AuthorizedClientService = InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository)

      val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
         .clientCredentials()
         .build();

      val oAuth2AuthorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, oAuth2AuthorizedClientService)
      oAuth2AuthorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)

      return restTemplateBuilder
         .additionalInterceptors(OAuthClientCredentialsRestTemplateInterceptor(oAuth2AuthorizedClientManager, clientRegistration))
         .build()
   }
}

