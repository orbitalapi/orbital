package com.orbitalhq.cockpit.core.security.authorisation

import com.orbitalhq.auth.authorisation.UserRole
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Path
import java.nio.file.Paths

@ConfigurationProperties(prefix = "vyne.security.authorisation")
class VyneAuthorisationConfig {
   val roleDefinitionsFile: Path = Paths.get("config/roles.conf")
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
   // See: KeycloakRolesExtractor.KeycloakJwtKind |PropelAuthClaimsExtractor.PropelAuthJwtKind
   val jwtType: String? = KeycloakRolesExtractor.KeycloakJwtKind
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
