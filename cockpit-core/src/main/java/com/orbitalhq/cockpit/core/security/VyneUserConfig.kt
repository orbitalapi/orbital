package com.orbitalhq.cockpit.core.security

import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Path
import java.nio.file.Paths

//@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.users")
data class VyneUserConfig(
   val configFile: Path = Paths.get("config/users.conf")
)
