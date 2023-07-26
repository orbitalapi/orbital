package io.vyne.connectors.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.vyne.config.ConfigSourceLoader
import io.vyne.config.FileConfigSourceLoader
import io.vyne.config.MergingHoconConfigRepository
import io.vyne.connectors.VyneConnectionsConfig
import java.nio.file.Path

class ConfigFileConnectorsRegistry(
   loaders: List<ConfigSourceLoader>,
   fallback: Config = ConfigFactory.systemEnvironment(),
) : MergingHoconConfigRepository<ConnectionsConfig>(loaders, fallback) {

   // for testing
   constructor(path: Path, fallback: Config = ConfigFactory.systemEnvironment()) : this(
      listOf(FileConfigSourceLoader(path, packageIdentifier = VyneConnectionsConfig.PACKAGE_IDENTIFIER)),
      fallback
   )

   override fun extract(config: Config): ConnectionsConfig = config.extract()

   override fun emptyConfig(): ConnectionsConfig {
      return ConnectionsConfig()
   }

   fun load(): ConnectionsConfig = typedConfig()

}


