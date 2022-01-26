package io.vyne.queryService.security.authorisation

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.core.io.ClassPathResource
import java.nio.file.Path
import java.nio.file.Paths

@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.security.authorisation")
class VyneAuthorisationConfig {
   val roleDefinitionsFile: Path = ClassPathResource("authorisation/vyne-authorisation-role-definitions.conf").file.toPath()
   // var for testing.
   var userToRoleMappingsFile: Path = Paths.get("user-role-mappings.conf")
   val adminRole: VyneUserAuthorisationRole = "Admin"
}
