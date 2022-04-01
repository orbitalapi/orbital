package io.vyne.connectors

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.nio.file.Path
import java.nio.file.Paths

@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.connections")
data class VyneConnectionsConfig(
   val configFile: Path = Paths.get("config/connections.conf")
)
