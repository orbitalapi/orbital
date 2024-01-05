package com.orbitalhq.cockpit.core.security.authorisation

import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Path
import java.nio.file.Paths

@ConfigurationProperties(prefix = "vyne.security.authorisation")
class VyneAuthorisationConfig {
   val roleDefinitionsFile: Path = Paths.get("config/roles.conf")
}

/**
 * Defines which type of token returned from the OIDC Authentication service
 * should be sent back to our server to verify the user.
 *
 * Default (per the spec) is Access.
 * In AWS Cognito, the Access token is pretty sparse, and the details we need
 * are in the Id token.
 *
 * Setting this informs the UI which token to send in the Authorization header
 */
enum class IdentityTokenKind {
   Access,
   Id
}


// configuration class annotation need to use kebab-case, otherwise spring gives prefix must be in canonical form in Intellij
@ConfigurationProperties(prefix = "vyne.security.open-idp")
data class VyneOpenIdpConnectConfig(
   val enabled: Boolean = false,
   // Open Idp issuer Url
   val issuerUrl: String? = null,
   // The client Id defined in Idp for Orbital.
   // Null if enabled = false
   val clientId: String? = null,
   // Scopes defined in Idp
   val scope: String = "openid profile email offline_access",
   // Require login via https
   val requireHttps: Boolean = true,
   val accountManagementUrl: String? = null,
   val orgManagementUrl: String? = null,
   val jwksUri: String? = null,
   val roles: JwtRolesConfig = JwtRolesConfig(),
   val identityTokenKind: IdentityTokenKind = IdentityTokenKind.Access,

   /**
    * The url to load the oidc discovery document from.
    * Normally is inferred from the issuerUrl
    * (ie., ${issuerUrl}/.well-known/openid-configuration)
    * However, some IDP's use a custom discovery url. (Azure).
    */
   val oidcDiscoveryUrl: String?,
) {
   init {
      val configErrors = mutableListOf<String>()

      fun idpProperty(key:String) = "vyne.security.open-idp.$key"
      fun appendPrefixedError(message: String) = configErrors.add("When ${idpProperty("enabled")} = true, $message")

      if (enabled) {
         if (clientId == null) appendPrefixedError("${idpProperty("client-id")} must be set")
         if (issuerUrl == null && oidcDiscoveryUrl == null) appendPrefixedError("either ${idpProperty("oidc-discovery-url")} or ${idpProperty("issuer-url")} must be set")
         if (configErrors.isNotEmpty()) {
            error(configErrors.joinToString("\n"))
         }
      }
   }
}


/**
 * Additional config settings for how to extract roles from OIDC JWT's.
 */
data class JwtRolesConfig(
   // See: KeycloakRolesExtractor.KeycloakJwtKind |PropelAuthClaimsExtractor.PropelAuthJwtKind | PathRolesExtractor
   val format: String = KeycloakRolesExtractor.KeycloakJwtRolesFormat,
   val path: String? = null
) {
   init {
       if (format == SimplePathBasedRolesExtractor.PathJwtKind && path == null) {
          error("When vyne.security.open-idp.roles.format = ${SimplePathBasedRolesExtractor.PathJwtKind} you must also specify vyne.security.open-idp.roles.path indicating the path within the token to read the roles from")
       }
   }
}
