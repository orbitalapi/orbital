package io.vyne.queryService.security.authorisation

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.nio.file.Path
import java.nio.file.Paths

@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.security.authorisation")
class VyneAuthorisationConfig {
   val roleDefinitionsFile: Path = Paths.get("roles.conf")
   // var for testing.
   var userToRoleMappingsFile: Path = Paths.get("user-role-mappings.conf")
   val adminRole: VyneUserAuthorisationRole = "Admin"

   /**
    * Idp Related Settings:
    */

}

@ConstructorBinding
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
