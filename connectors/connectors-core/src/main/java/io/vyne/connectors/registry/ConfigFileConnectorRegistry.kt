package io.vyne.connectors.registry

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.vyne.config.BaseHoconConfigFileRepository
import io.vyne.config.toConfig
import mu.KotlinLogging
import org.http4k.quoted
import java.nio.file.Files
import java.nio.file.Path

// Marker interface, to help make this stuff easier to follow
interface ConnectionConfigMap

abstract class ConfigFileConnectorRegistry<TMapType : ConnectionConfigMap, TConfigType : ConnectorConfiguration>(
   path: Path,
   fallback: Config = ConfigFactory.systemProperties(),
   val connectorConfigPrefix: String
) : BaseHoconConfigFileRepository<TMapType>(path, fallback) {
   private val logger = KotlinLogging.logger {}
   init {
       logger.info { "Using a connection config file at ${path.toFile().canonicalPath}" }
      if (!Files.exists(path)) {
         logger.info { "Connection config file at ${path.toFile().canonicalPath} doesn't exist, and will be created if required" }
      }
   }

   protected abstract fun getConnectionMap():Map<String,TConfigType>

   fun saveConnectorConfig(connectionConfig: TConfigType) {
      val newConfig = ConfigFactory.empty()
         .withValue(configPath(connectionConfig.connectionName), connectionConfig.toConfig().root())

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
}

enum class ConnectorType {
   JDBC,
   MESSAGE_BROKER,
   AWS,
   AWS_S3
}

/**
 * This is really a DTO.  Connectors can have lots of properties, and we only want to expose
 * the basic ones to the Web UI.
 */
data class ConnectorConfigurationSummary(
   private val connectorConfiguration: ConnectorConfiguration
) : ConnectorConfiguration by connectorConfiguration
