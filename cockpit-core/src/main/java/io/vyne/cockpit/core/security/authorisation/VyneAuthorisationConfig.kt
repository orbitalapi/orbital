package io.vyne.cockpit.core.security.authorisation

import io.vyne.auth.authorisation.VyneUserAuthorisationRole
import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Path
import java.nio.file.Paths

@ConfigurationProperties(prefix = "vyne.security.authorisation")
class VyneAuthorisationConfig {
   val roleDefinitionsFile: Path = Paths.get("config/roles.conf")
   // var for testing.
   var userToRoleMappingsFile: Path = Paths.get("config/user-role-mappings.conf")
   val adminRole: VyneUserAuthorisationRole = "Admin"

   /**
    * Idp Related Settings:
    */

}

// configuration class annotation need to use kebab-case, otherwise spring gives prefix must be in canonical form in Intellij
@ConfigurationProperties(prefix = "vyne.security.open-idp")
data class VyneOpenIdpConnectConfig(
   val enabled: Boolean = false,
   // Open Idp issuer Url
   val issuerUrl: String = "",
   // The client Id defined in Idp for Vyne.
   val clientId: String = "",
   // Scopes defined in Idp
   val scope: String = ""
)
