package com.orbitalhq.spring.http.auth.schemes

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.orbitalhq.auth.schemes.OAuth2
import com.orbitalhq.schemas.ServiceName
import mu.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Instant

/**
 * Handles fetching and refreshing access tokens for services using OAuth refresh token flow.
 *
 * Specifically, this is for use with clients where issuance of the original authorization code
 * is not handled within the auth exchange, but manually.  ie., instances where the refresh_token is obtained
 * and explicitly provided to Orbital
 *
 * eg: https://www.zoho.com/crm/developer/docs/api/v4/access-refresh.html
 *
 * Design choice: I've tried very hard to get Spring's ServerOAuth2AuthorizedClientExchangeFilterFunction
 * to handle this flow, but couldn't get it to work.  That seems more geared
 * towards the ClientCredentials flow, where the client_id and client_secret are exchanged
 * for an access token. It seems that refresh token flows are supported, but the Spring OAuthClient
 * wants to control the entire flow, including the original request for the token.
 *  *
 * Instead, in this flow, the client_id, client_secret and a pre-determined refresh_token
 * are used to obtain and refresh access tokens.
 *
 */
class RefreshTokenExchangeFilterFunction(
   private val serviceName: ServiceName,
   private val oauthScheme: OAuth2,
   private val clientService: ReactiveOAuth2AuthorizedClientService,
   private val webClient: WebClient = WebClient.create(),
   private val objectMapper: ObjectMapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
) : ExchangeFilterFunction {
   init {
      require(oauthScheme.refreshToken != null) { "Cannot initialize a RefreshTokenExchangeFilterFunction without a refreshToken present on the OAuth configuration" }
   }

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> {
      return loadAuthorizedClient()
         .onErrorResume { error ->
            logger.warn { "An exception was thrown loading an authorized client for service $serviceName: ${error::class.simpleName} - ${error.message}" }
            Mono.empty()
         }
//         .switchIfEmpty(Mono.fromCallable {
//            error("No authorization client exists for service $serviceName - however one was expected. Cannot issue / reissue authentication token.")
//         })
         .flatMap { client ->
            val reauthenticationRequest =
               if (isExpiring(client.accessToken)) {
                  reauthenticate(oauthScheme)
               } else {
                  Mono.just(client)
               }

            reauthenticationRequest.flatMap { authenticatedClient ->
               val requestWithBearerToken = ClientRequest.from(request)
                  .header(
                     HttpHeaders.AUTHORIZATION,
                     "${authenticatedClient.accessToken.tokenType.value} ${authenticatedClient.accessToken.tokenValue}"
                  )
                  .build()
               next.exchange(requestWithBearerToken)
            }

         }
   }

   private fun loadAuthorizedClient(): Mono<OAuth2AuthorizedClient> {
      return clientService.loadAuthorizedClient(serviceName, OAuth2Utils.ANONYMOUS_USER)
   }

   private fun reauthenticate(oauthScheme: OAuth2): Mono<OAuth2AuthorizedClient> {
      val uri =
         "${oauthScheme.accessTokenUrl}?refresh_token=${oauthScheme.refreshToken!!}&client_id=${oauthScheme.clientId}&client_secret=${oauthScheme.clientSecret}&grant_type=refresh_token"
      logger.debug { "Sending request to reauthenticate service $serviceName to ${oauthScheme.accessTokenUrl}" }
      return webClient.post()
         .uri(uri)
         .exchangeToMono { clientResponse ->
            val errorMessage = "Attempt to authenticate $serviceName at ${oauthScheme.accessTokenUrl} failed with error code ${clientResponse.statusCode()}"
            when {
                clientResponse.statusCode().is4xxClientError -> {
                   logger.warn { errorMessage }
                   Mono.error(BadCredentialsException(errorMessage))
                }
               clientResponse.statusCode().is5xxServerError -> {
                  logger.warn { errorMessage }
                  Mono.error(AuthenticationServiceException(errorMessage))
               }
                else -> {
                   clientResponse.bodyToMono(Map::class.java)
                      .flatMap { responseMap -> convertResponseToAuthentication(responseMap) }
                }
            }

         }
         .onErrorResume { e ->
            throw e
         }
         .flatMap { response ->
            val (principal, authorizedClient) = oauthScheme.createAuthorizedClient(
               serviceName,
               response.access_token,
               Instant.now(),
               response.expires()
            )
            clientService.saveAuthorizedClient(
               authorizedClient, principal
            ).then(loadAuthorizedClient())
         }
   }

   /**
    * Reads the response map, and attempts to convert to a RefreshTokenResponse.
    *
    * Unfortunately, some OAuth clients return a 200 response (indicating success), with an
    * error payload, so we need to actually read the body to determine what's happened.
    *
    */
   private fun convertResponseToAuthentication(responseMap: Map<*, *>): Mono<RefreshTokenResponse> {
      return when {
         responseMap.containsKey("error") -> {
            val errorCode = responseMap["error"]!! as String
            logger.info { "Attempt to refresh token for service $serviceName failed with error $errorCode" }
            Mono.error(OAuth2AuthenticationException(errorCode))
         }

         else -> try {
            val refreshTokenResponse = objectMapper
               .convertValue<RefreshTokenResponse>(responseMap)
            logger.debug { "Attempt to refresh token for service $serviceName completed successfully" }
            Mono.just(refreshTokenResponse)
         } catch (e: Exception) {
            logger.warn { "Failed to read response body of attempt to refresh token - Exception thrown ${e::class.simpleName} - ${e.message}. Received: $responseMap" }
            Mono.error(
               AuthenticationServiceException(
                  "Failed to convert response from OAuth refresh token exchange: ${e.message}", e
               )
            )
         }
      }
   }

   private fun isExpiring(token: OAuth2AccessToken): Boolean {
      return token.expiresAt != null && token.expiresAt!!.isBefore(Instant.now().minusSeconds(60))
   }
}

data class RefreshTokenResponse(
   val access_token: String,
   val token_type: String,
   val expires_in: Int
) {
   fun expires(from: Instant = Instant.now()) = from.plusSeconds(expires_in.toLong())
}
