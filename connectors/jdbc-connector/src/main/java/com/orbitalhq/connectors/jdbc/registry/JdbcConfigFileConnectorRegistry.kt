package com.orbitalhq.connectors.jdbc.registry

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.connectors.config.jdbc.DefaultJdbcConnectionConfiguration
import com.orbitalhq.connectors.config.jdbc.JdbcConnectionConfiguration
import com.orbitalhq.connectors.registry.ConfigFileConnectorRegistry
import com.orbitalhq.connectors.registry.ConnectionConfigMap
import com.orbitalhq.connectors.registry.MutableConnectionRegistry
import java.nio.file.Path

//@Deprecated("Use SourceLoaderJdbcConnectionRegistry instead")
//class JdbcConfigFileConnectorRegistry(path: Path, fallback: Config = ConfigFactory.systemEnvironment()) :
//    JdbcConnectionRegistry, MutableConnectionRegistry<JdbcConnectionConfiguration>,
//   ConfigFileConnectorRegistry<JdbcConnections, DefaultJdbcConnectionConfiguration>(
//      path,
//      fallback,
//      JdbcConnections.CONFIG_PREFIX
//   ) {
//
//   override fun remove(targetPackage: PackageIdentifier, connectionName: String) {
//      TODO("Not yet implemented")
//   }
//
//   override fun extract(config: Config): JdbcConnections = config.extract()
//   override fun emptyConfig(): JdbcConnections = JdbcConnections()
//   override fun getConnectionMap(): Map<String, DefaultJdbcConnectionConfiguration> {
//      return this.typedConfig().jdbc
//   }
//
//   override fun register(targetPackage: PackageIdentifier, connectionConfiguration: JdbcConnectionConfiguration) {
//      require(connectionConfiguration is DefaultJdbcConnectionConfiguration) { "Only DefaultJdbcConnectionConfiguration is supported by this class" }
//      saveConnectorConfig(connectionConfiguration)
//   }
//
//   override fun listAll(): List<JdbcConnectionConfiguration> {
//      return listConnections()
//   }
//
//   override fun remove(targetPackage: PackageIdentifier, connectionConfiguration: JdbcConnectionConfiguration) {
//      this.removeConnectorConfig(connectionConfiguration.connectionName)
//   }
//
//
//}

data class JdbcConnections(
   val jdbc: MutableMap<String, DefaultJdbcConnectionConfiguration> = mutableMapOf()
) : ConnectionConfigMap {
   companion object {
      val CONFIG_PREFIX = JdbcConnections::jdbc.name  // must match the name of the param in the constructor
   }
}
