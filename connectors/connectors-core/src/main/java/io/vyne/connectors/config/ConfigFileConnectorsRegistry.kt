package io.vyne.connectors.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.ClassContainer
import io.github.config4k.CustomType
import io.github.config4k.extract
import io.vyne.config.BaseHoconConfigFileRepository
import io.vyne.connectors.config.jdbc.DefaultJdbcConnectionConfiguration
import io.vyne.connectors.config.jdbc.JdbcConnectionConfiguration
import java.nio.file.Path
import kotlin.reflect.full.isSubclassOf

class ConfigFileConnectorsRegistry(
   path: Path,
   fallback: Config = ConfigFactory.systemEnvironment(),
) : BaseHoconConfigFileRepository<ConnectorsConfig>(path, fallback) {
   init {

   }
   override fun extract(config: Config): ConnectorsConfig = config.extract()

   override fun emptyConfig(): ConnectorsConfig {
      return ConnectorsConfig()
   }

   fun load(): ConnectorsConfig = typedConfig()

}


