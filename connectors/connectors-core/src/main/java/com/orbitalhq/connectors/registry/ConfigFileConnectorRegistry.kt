package com.orbitalhq.connectors.registry

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.orbitalhq.config.BaseHoconConfigFileRepository
import com.orbitalhq.config.toHocon
import mu.KotlinLogging
import org.http4k.quoted
import java.nio.file.Files
import java.nio.file.Path

// Marker interface, to help make this stuff easier to follow
interface ConnectionConfigMap

@Deprecated("Replacing this, as it makes serialization difficult - use config.ConfigFileConnectorsRegistry")
abstract class ConfigFileConnectorRegistry<TMapType : ConnectionConfigMap, TConfigType : ConnectorConfiguration>(
   path: Path,
   fallback: Config = ConfigFactory.systemEnvironment(),
   val connectorConfigPrefix: String
) : BaseHoconConfigFileRepository<TMapType>(path, fallback) {
   private val logger = KotlinLogging.logger {}

   init {
      logger.info { "Using a connection config file at ${path.toFile().canonicalPath}" }
      if (!Files.exists(path)) {
         logger.info { "Connection config file at ${path.toFile().canonicalPath} doesn't exist, and will be created if required" }
      }
   }

   abstract fun getConnectionMap(): Map<String, TConfigType>

   fun saveConnectorConfig(connectionConfig: TConfigType) {
      val newConfig = ConfigFactory.empty()
         .withValue(configPath(connectionConfig.connectionName), connectionConfig.toHocon().root())

      // Use the existing unresolvedConfig to ensure that when we're
      // writing back out, that tokens that have been resolved
      // aren't accidentally written with their real values back out
      val existingValues = unresolvedConfig()

      val updated = ConfigFactory.empty()
         .withFallback(newConfig)
         .withFallback(existingValues)

      saveConfig(updated)
   }

   fun removeConnectorConfig(name: String) {
      saveConfig(
         unresolvedConfig()
            .withoutPath(configPath(name))
      )
   }

   fun listConnections(): List<TConfigType> {
      return getConnectionMap().values.toList()
   }

   fun getConnection(name: String): TConfigType {
      return getConnectionMap()[name] ?: error("No connection $name is defined")
   }

   fun hasConnection(name: String): Boolean {
      return getConnectionMap().containsKey(name)
   }

   private fun configPath(connectionName: String): String {
      return "$connectorConfigPrefix.${connectionName.quoted()}"
   }


}

interface ConnectorConfiguration {
   val connectionName: String
   val driverName: String
   val type: ConnectorType

   /**
    * Returns properties for display in the UI.
    * Anything sensitive should be obscured. (Use
    * maps.obscureKeys()
    */
   fun getUiDisplayProperties(): Map<String, Any>
}

enum class ConnectorType {
   JDBC,
   MESSAGE_BROKER,
   AWS,
   AWS_S3,
   AZURE_STORAGE
}

/**
 * This is really a DTO.  Connectors can have lots of properties, and we only want to expose
 * the basic ones to the Web UI.
 */
data class ConnectorConfigurationSummary(
   val connectionName: String,
   val connectionType: ConnectorType,
   val driverName: String,
   val properties: Map<String, Any>
) {
   constructor(config: ConnectorConfiguration) : this(
      config.connectionName, config.type, config.driverName, config.getUiDisplayProperties()
   )
}
