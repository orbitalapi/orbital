package io.vyne.queryService.security

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.nio.file.Path
import java.nio.file.Paths

@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.users")
data class VyneUserConfig(
   val configFile: Path = Paths.get("config/users.conf")
)
