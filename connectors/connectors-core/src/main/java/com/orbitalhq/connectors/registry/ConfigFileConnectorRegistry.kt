package com.orbitalhq.connectors.registry

import com.orbitalhq.Message
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.ResultWithMessage
import com.orbitalhq.config.BaseHoconConfigFileRepository
import com.orbitalhq.config.toHocon
import com.orbitalhq.connections.ConnectionStatus
import com.orbitalhq.schemas.SchemaMemberReference
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.ClassContainer
import io.github.config4k.CustomType
import io.github.config4k.toConfig
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import org.http4k.quoted
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

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
   val default: Boolean
      get() = false
}

@Serializable
data class ConfigurationFilePath(val path: Path) {
   companion object {
      fun parse(input: String,  config: Config): ConfigurationFilePath {
         val origin = config.origin()
        val path =  if (origin?.filename() != null && !Paths.get(input).isAbsolute) {
            val configFilePath = Paths.get(origin.filename())
            configFilePath.parent.resolve(input)
         } else {
            Paths.get(input)
         }
         return ConfigurationFilePath(path)
      }
   }
}

/**
 * A custom Config4k to handle the relative path resolution
 * Config4 has built-in support for Path and File members for config POJOs, but they don't support
 * relative paths, see - https://github.com/config4k/config4k/blob/main/src/main/kotlin/io/github/config4k/readers/FileReader.kt
 */
class ConfigurationFilePathCustomType: CustomType {
   override fun parse(clazz: ClassContainer, config: Config, name: String): Any? {
      return ConfigurationFilePath.parse(config.getString(name), config)
   }

   override fun testParse(clazz: ClassContainer): Boolean {
      return  clazz.mapperClass == ConfigurationFilePath::class
   }

   override fun testToConfig(obj: Any): Boolean {
      return ConfigurationFilePath::class.isInstance(obj)
   }

   override fun toConfig(obj: Any, name: String): Config {
      return (obj as ConfigurationFilePath).path.absolutePathString().toConfig(name)
   }
}

enum class ConnectorType {
   JDBC,
   MESSAGE_BROKER,
   AWS,
   AWS_S3,
   AZURE_STORAGE,
   CACHE,
   NO_SQL
}

/**
 * Another DTO, returned when users click on the "detail"
 * view of a connection
 */
data class ConnectorConfigDetail(
   val config: ConnectorConfigurationSummary,
   val usages: List<SchemaMemberReference>
)


/**
 * This is really a DTO.  Connectors can have lots of properties, and we only want to expose
 * the basic ones to the Web UI.
 */
data class ConnectorConfigurationSummary(
   val connectionName: String,
   val connectionType: ConnectorType,
   val driverName: String,
   val properties: Map<String, Any>,
   val packageIdentifier: PackageIdentifier,
   val connectionStatus: ConnectionStatus,
   val usages: List<SchemaMemberReference>?,
   // used when performing a mutation (adding / removing a connection)
   override val messages: List<Message> = emptyList()
):ResultWithMessage {
   constructor(
      packageIdentifier: PackageIdentifier,
      config: ConnectorConfiguration,
      connectionStatus: ConnectionStatus = ConnectionStatus.unknown(),
      connectionUIDisplayProvider: ConnectionUIDisplayProvider? = null,
      usages: List<SchemaMemberReference>? = null,
      messages: List<Message> = emptyList()
   ) : this(
      config.connectionName, config.type, config.driverName,
      connectionUIDisplayProvider?.uiProps(config) ?: config.getUiDisplayProperties(),
      packageIdentifier, connectionStatus, usages,
      messages
   )
}

