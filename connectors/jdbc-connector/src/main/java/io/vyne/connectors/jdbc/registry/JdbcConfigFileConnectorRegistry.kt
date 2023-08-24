package io.vyne.connectors.jdbc.registry

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.vyne.connectors.config.jdbc.DefaultJdbcConnectionConfiguration
import io.vyne.connectors.config.jdbc.JdbcConnectionConfiguration
import io.vyne.connectors.registry.ConfigFileConnectorRegistry
import io.vyne.connectors.registry.ConnectionConfigMap
import io.vyne.connectors.registry.MutableConnectionRegistry
import java.nio.file.Path

@Deprecated("Use SourceLoaderJdbcConnectionRegistry instead")
class JdbcConfigFileConnectorRegistry(path: Path, fallback: Config = ConfigFactory.systemEnvironment()) :
    JdbcConnectionRegistry, MutableConnectionRegistry<JdbcConnectionConfiguration>,
   ConfigFileConnectorRegistry<JdbcConnections, DefaultJdbcConnectionConfiguration>(
      path,
      fallback,
      JdbcConnections.CONFIG_PREFIX
   ) {

   override fun extract(config: Config): JdbcConnections = config.extract()
   override fun emptyConfig(): JdbcConnections = JdbcConnections()
   override fun getConnectionMap(): Map<String, DefaultJdbcConnectionConfiguration> {
      return this.typedConfig().jdbc
   }

   override fun register(connectionConfiguration: JdbcConnectionConfiguration) {
      require(connectionConfiguration is DefaultJdbcConnectionConfiguration) { "Only DefaultJdbcConnectionConfiguration is supported by this class" }
      saveConnectorConfig(connectionConfiguration)
   }

   override fun listAll(): List<JdbcConnectionConfiguration> {
      return listConnections()
   }

   override fun remove(connectionConfiguration: JdbcConnectionConfiguration) {
      this.removeConnectorConfig(connectionConfiguration.connectionName)
   }


}

data class JdbcConnections(
   val jdbc: MutableMap<String, DefaultJdbcConnectionConfiguration> = mutableMapOf()
) : ConnectionConfigMap {
   companion object {
      val CONFIG_PREFIX = JdbcConnections::jdbc.name  // must match the name of the param in the constructor
   }
}
