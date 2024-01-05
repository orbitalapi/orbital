package com.orbitalhq.cockpit.core.security

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.orbitalhq.cockpit.core.security.authorisation.IdentityTokenKind
import com.orbitalhq.cockpit.core.security.authorisation.VyneOpenIdpConnectConfig
import mu.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntity
import reactor.core.publisher.Mono

/**
 * Service which exposes config settings to the UI.
 */
@RestController
class SecurityConfigController(
   private val config: FrontEndSecurityConfig
) {

   /**
    * Return the client side related Idp configuration, so that vyne client can initialise Authorization Code flow.
    */
   @GetMapping("/api/security/config")
   fun securityIssuerUrl() = config

}

data class FrontEndSecurityConfig(
   val issuerUrl: String?, // null if security is disabled
   /**
    * The url to load the oidc discovery document from.
    * Normally is inferred from the issuerUrl
    * (ie., ${issuerUrl}/.well-known/openid-configuration)
    * However, some IDP's use a custom discovery url. (Azure).
    */
   val oidcDiscoveryUrl: String?,
   val clientId: String?,
   val scope: String?,
   val requireLoginOverHttps: Boolean,
   val redirectUri: String? = null,
   val enabled: Boolean = issuerUrl != null,
   val accountManagementUrl: String?,
   val orgManagementUrl: String?,
   val identityTokenKind: IdentityTokenKind = IdentityTokenKind.Access,
   val jwksUri: String?
) {
   companion object {
      fun disabled(): FrontEndSecurityConfig {
         return FrontEndSecurityConfig(
            enabled = false,
            oidcDiscoveryUrl = null,
            clientId = null,
            scope = null,
            requireLoginOverHttps = true,
            redirectUri = null,
            accountManagementUrl = null,
            orgManagementUrl = null,
            jwksUri = null,
            issuerUrl = null
         )

      }
   }
}


