package io.vyne.connectors.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.vyne.config.FileHoconLoader
import io.vyne.config.HoconLoader
import io.vyne.config.MergingHoconConfigRepository
import java.nio.file.Path

class ConfigFileConnectorsRegistry(
   loaders: List<HoconLoader>,
   fallback: Config = ConfigFactory.systemEnvironment(),
) : MergingHoconConfigRepository<ConnectionsConfig>(loaders, fallback) {

   // for testing
   constructor(path: Path, fallback: Config = ConfigFactory.systemEnvironment()) : this(
      listOf(FileHoconLoader(path)),
      fallback
   )

   override fun extract(config: Config): ConnectionsConfig = config.extract()

   override fun emptyConfig(): ConnectionsConfig {
      return ConnectionsConfig()
   }

   fun load(): ConnectionsConfig = typedConfig()

}


