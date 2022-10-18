package io.orbital.station.security

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.nio.file.Path
import java.nio.file.Paths

@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.users")
data class OrbitalUserConfig(
   val configFile: Path = Paths.get("config/users.conf")
)
