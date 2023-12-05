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
      FrontEndSecurityConfig(
         openIdpConfiguration.issuerUrl,
         openIdpConfiguration.clientId,
         openIdpConfiguration.scope,
         openIdpConfiguration.requireHttps,
         accountManagementUrl =  openIdpConfiguration.accountManagementUrl,
         orgManagementUrl = openIdpConfiguration.orgManagementUrl
         )

}

data class FrontEndSecurityConfig(
   val issuerUrl: String?, // null if security is disabled
   val clientId: String,
   val scope: String,
   val requireLoginOverHttps: Boolean,
   val redirectUri: String? = null,
   val enabled: Boolean = issuerUrl != null,
   val accountManagementUrl: String?,
   val orgManagementUrl:String?
)


