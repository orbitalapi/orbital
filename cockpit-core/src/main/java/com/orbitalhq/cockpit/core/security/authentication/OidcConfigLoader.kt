package com.orbitalhq.cockpit.core.security.authentication

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.orbitalhq.cockpit.core.security.FrontEndSecurityConfig
import com.orbitalhq.cockpit.core.security.authorisation.VyneOpenIdpConnectConfig
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntity
import reactor.core.publisher.Mono

/**
 * Builds the OIDC config required for the front end and OIDC
 * support.
 *
 * If params weren't provided manually (via config), then
 * we load the OpenId Config  from the IDP, either using the
 * explict oidcDiscoveryUrl, or by convention inferred from the issuerUrl
 */
@Configuration
class OidcConfigLoader {

   companion object {
      private val logger = KotlinLogging.logger {}
   }


   @Bean
   fun buildFrontEndConfig(
      openIdpConfiguration: VyneOpenIdpConnectConfig,
      webClientBuilder: WebClient.Builder
   ): FrontEndSecurityConfig {
      if (!openIdpConfiguration.enabled) {
         return FrontEndSecurityConfig.disabled()
      }
      val oidcDiscoveryUrl = openIdpConfiguration.oidcDiscoveryUrl ?: openIdpConfiguration.issuerUrl?.let {  "${it.removeSuffix("/")}/.well-known/openid-configuration" }
      require(oidcDiscoveryUrl != null) { "Either oidcDiscoveryUrl or issuerUrl is required"}

      return if (openIdpConfiguration.jwksUri != null && openIdpConfiguration.issuerUrl != null) {
         return FrontEndSecurityConfig(
            issuerUrl = openIdpConfiguration.issuerUrl,
            oidcDiscoveryUrl = openIdpConfiguration.oidcDiscoveryUrl,
            clientId = openIdpConfiguration.clientId!!,
            scope = openIdpConfiguration.scope,
            requireLoginOverHttps = openIdpConfiguration.requireHttps,
            accountManagementUrl = openIdpConfiguration.accountManagementUrl,
            orgManagementUrl = openIdpConfiguration.orgManagementUrl,
            identityTokenKind = openIdpConfiguration.identityTokenKind,
            jwksUri = openIdpConfiguration.jwksUri
         )
      } else{
         val mapper = jacksonObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

         val webClient = webClientBuilder.build()

         loadOidcConfig(oidcDiscoveryUrl, webClient, mapper)
            .map { (oidcDiscoveryUrl, openIdConfig) ->
               FrontEndSecurityConfig(
                  // Read from the loaded config
                  issuerUrl = openIdConfig.issuer,
                  jwksUri = openIdConfig.jwksUri,

                  // Read from the user-provided config
                  oidcDiscoveryUrl = oidcDiscoveryUrl,
                  clientId = openIdpConfiguration.clientId!!,
                  scope = openIdpConfiguration.scope,
                  requireLoginOverHttps = openIdpConfiguration.requireHttps,
                  accountManagementUrl = openIdpConfiguration.accountManagementUrl,
                  orgManagementUrl = openIdpConfiguration.orgManagementUrl,
                  identityTokenKind = openIdpConfiguration.identityTokenKind,
               )
            }.block()!!
      }
   }

   private fun loadOidcConfig(
      oidcDiscoveryUrl: String,
      webClient: WebClient,
      objectMapper: ObjectMapper
   ): Mono<Pair<String, OpenIdConfiguration>> {
      logger.info { "Loading OIDC Config doc from $oidcDiscoveryUrl" }
      return webClient.get()
         .uri(oidcDiscoveryUrl)
         .retrieve()
         .toEntity<Map<String, Any>>()
         .map {
            try {
               objectMapper.convertValue<OpenIdConfiguration>(it.body!!)
            } catch (e: Exception) {
               val message = "Failed to read the OIDC configuration doc from $oidcDiscoveryUrl: ${e.message}"
               logger.error { message }
               throw RuntimeException(message, e)
            }
         }.map { oidcDiscoveryUrl to it }
   }

}


/**
 * Models the data returned from as .well-known/openid-configuration endpoint
 */
data class OpenIdConfiguration(
   val tokenEndpoint: String,
   val jwksUri: String,
   val scopesSupported: List<String>,
   val issuer: String,
   val authorizationEndpoint: String?,
   val userinfoEndpoint: String?
)
