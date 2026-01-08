package com.orbitalhq.cockpit.core.security.authentication.oidc

import com.nimbusds.oauth2.sdk.id.Issuer
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata
import com.orbitalhq.auth.authentication.ExecutionPrincipalAuthenticationService
import com.orbitalhq.auth.authentication.VyneUser
import com.orbitalhq.auth.authentication.toVyneUser
import com.orbitalhq.cockpit.core.security.authorisation.ClientAuthenticationType
import com.orbitalhq.cockpit.core.security.authorisation.VyneOpenIdpConnectConfig
import com.orbitalhq.schema.publisher.http.RetryConfig
import mu.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.security.Principal
import java.time.Duration
import java.util.function.Consumer

private val logger = KotlinLogging.logger {}
/**
 * Loads a token for Oribtal's execution principal from
 * a configured OAuth IDP
 */
class OAuthExecutionPrincipalAuthService(
   private val securityConfig: VyneOpenIdpConnectConfig,
   private val webClient: WebClient,
   private val authenticationManager: ReactiveAuthenticationManager
) : ExecutionPrincipalAuthenticationService {

   private val tokenRequestUrl = extractExecutionPrincipalTokenRequestUrl()
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

   private fun extractExecutionPrincipalTokenRequestUrl(): String {
      return securityConfig.executorRoleTokenUrl ?: extractExecutionPrincipalTokenRequestUrlFromWellKnownOpenIdc()
   }

   private fun extractExecutionPrincipalTokenRequestUrlFromWellKnownOpenIdc(): String {
      val issuerUrl =  securityConfig.oidcDiscoveryUrl ?: securityConfig.issuerUrl ?: throw IllegalArgumentException("Either oidcDiscoveryUrl or issuerUrl must be provided!")
      val issuer = Issuer(issuerUrl)
      logger.info { "Fetching the OpenId Configuration from => ${OIDCProviderMetadata.resolveURL(issuer)}" }
      val retryTemplate = RetryConfig.simpleRetryWithBackoff(Duration.ofSeconds(15), "fetching-oidc-config")
      return retryTemplate.execute<String, Exception> {
         OIDCProviderMetadata.resolve(issuer).tokenEndpointURI.toString()
      }
   }

   private fun fetchToken(): Mono<String> {
      return when(securityConfig.executorRoleAuthenticationType) {
         ClientAuthenticationType.ClientSecretPost -> doFetch { headers ->
            headers.contentType = MediaType.APPLICATION_FORM_URLENCODED
         }
         ClientAuthenticationType.ClientSecretBasic -> doFetch { headers ->
            headers.setBasicAuth(securityConfig.executorRoleClientId, securityConfig.executorRoleClientSecret)
            headers.contentType = MediaType.APPLICATION_FORM_URLENCODED
         }
      }
   }

   private fun doFetch(headersConsumer: Consumer<HttpHeaders>): Mono<String> {
      logger.info { "Attempting to fetch authentication token for execution principal using ${securityConfig.executorRoleAuthenticationType}" }
      logger.info { "Token Url => $tokenRequestUrl" }
      return webClient.post()
         .uri(tokenRequestUrl)
         .headers(headersConsumer)
         .bodyValue(requestBody())
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
   private fun requestBody(): String {
      val stringBuilder = StringBuilder()
      stringBuilder.append("grant_type=client_credentials")
      if (securityConfig.executorRoleAuthenticationType == ClientAuthenticationType.ClientSecretPost) {
         stringBuilder.append("&client_id=${securityConfig.executorRoleClientId}&client_secret=${securityConfig.executorRoleClientSecret}")
      }

      if (securityConfig.executorRoleScopes != null) {
         stringBuilder.append("&scope=${securityConfig.executorRoleScopes}")
      }
      return stringBuilder.toString()
   }
}
