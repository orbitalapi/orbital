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
   val clientId: String = "orbital",
   // Scopes defined in Idp
   val scope: String = "openid profile email offline_access",
   // Require login via https
   val requireHttps: Boolean = true,
   val accountManagementUrl: String? = null,
   val orgManagementUrl: String? = null,
   val jwksUri: String? = null,
   val roles: JwtRolesConfig = JwtRolesConfig(),
   val identityTokenKind: IdentityTokenKind = IdentityTokenKind.Access
) {
   init {
      if (enabled) {
         val configErrors = listOf(
            "jwks-uri" to jwksUri,
            "issuer-url" to issuerUrl
         ).mapNotNull { (configKey, value) ->
            if (value == null) {
               "When vyne.security.open-idp.enabled = true, you must also set vyne.security.open-idp.$configKey"
            } else null
         }
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
