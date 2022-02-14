package io.vyne.spring.http.client.secure

import mu.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.registration.ClientRegistration


private val logger = KotlinLogging.logger {  }

/**
 * A RestTemplate interceptor to add Authorization header information to outgoing Http Request.
 * @param clientRegistration Holds the 'client id' attribute of the 'client' setup that is done at the Authorisation Server for OAuth2 Client Credentials flow.
 * @param manager For the given clientRegistration it encapsulates the interaction between the client application and the authorisation server for client credentials
 * flow. Behind the scene, it will issue a client credentials grant request (see https://www.oauth.com/oauth2-servers/access-tokens/client-credentials/ ) to authorisation
 * server token end point given by the clientRegistration and returns the access_token from the Authorisation Server response ( see https://www.oauth.com/oauth2-servers/access-tokens/access-token-response/ )
 *
 * Retrieved access_token is added as the Authorization header value (with Bearer Prefix)
 */
class OAuthClientCredentialsRestTemplateInterceptor(
   private val manager: OAuth2AuthorizedClientManager,
   private val clientRegistration: ClientRegistration
): ClientHttpRequestInterceptor {
   private val principal: Authentication = createPrincipal()
   override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
      val oAuth2AuthorizeRequest = OAuth2AuthorizeRequest
         .withClientRegistrationId(clientRegistration.registrationId)
         .principal(principal)
         .build()

      val client = manager.authorize(oAuth2AuthorizeRequest)
      logger.info { "invoking vyne with bearer token ${client.accessToken.tokenValue}" }
      request.headers.add(HttpHeaders.AUTHORIZATION, "Bearer ${client.accessToken.tokenValue}")
      return execution.execute(request, body)
   }

   private fun createPrincipal(): Authentication {
      return object : Authentication {
         override fun getAuthorities(): Collection<GrantedAuthority?> {
            return emptyList()
         }

         override fun getCredentials(): Any? {
            return null
         }

         override fun getDetails(): Any? {
            return null
         }

         override fun getPrincipal(): Any {
            return this
         }

         override fun isAuthenticated(): Boolean {
            return false
         }

         @Throws(IllegalArgumentException::class)
         override fun setAuthenticated(isAuthenticated: Boolean) {
         }

         override fun getName(): String {
            return clientRegistration.clientId
         }
      }
   }
}
