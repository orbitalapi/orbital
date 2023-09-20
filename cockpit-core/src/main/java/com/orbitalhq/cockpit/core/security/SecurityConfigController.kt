package com.orbitalhq.cockpit.core.security

import com.orbitalhq.cockpit.core.security.authorisation.VyneOpenIdpConnectConfig
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Service which exposes config settings to the UI.
 */
@RestController
class SecurityConfigController(private val openIdpConfiguration: VyneOpenIdpConnectConfig) {

   /**
    * Return the client side related Idp configuration, so that vyne client can initialise Login Implicit Flow.
    * see https://auth0.com/docs/get-started/authentication-and-authorization-flow/implicit-flow-with-form-post
    */
   @GetMapping("/api/security/config")
   fun securityIssuerUrl() =
      FrontendConfig(openIdpConfiguration.issuerUrl, openIdpConfiguration.clientId, openIdpConfiguration.scope)

}

data class FrontendConfig(
   val issuerUrl: String,
   val clientId: String,
   val scope: String,
   val enabled: Boolean = issuerUrl.isNotBlank()
)


