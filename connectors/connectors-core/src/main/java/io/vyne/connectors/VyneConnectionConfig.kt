package io.vyne.connectors

import io.vyne.PackageIdentifier
import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Path
import java.nio.file.Paths

//@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.connections")
data class VyneConnectionsConfig(
   val configFile: Path = Paths.get("config/connections.conf")
) {
   companion object {
      /**
       * Package id for the config we load at the system level (not for loading from config
       * sitting inside packages)
       */
      val PACKAGE_IDENTIFIER = PackageIdentifier.fromId("com.orbitalhq.config/connections/1.0.0")
   }
}


