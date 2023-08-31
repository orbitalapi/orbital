package io.vyne.connectors.registry

import com.typesafe.config.ConfigFactory
import io.github.config4k.toConfig
import io.vyne.PackageIdentifier
import io.vyne.connectors.config.ConnectionsConfig
import io.vyne.connectors.config.SourceLoaderConnectorsRegistry
import kotlin.reflect.KProperty1

interface ConnectionRegistry<T : ConnectorConfiguration> {
   fun hasConnection(name: String): Boolean
   fun getConnection(name: String): T
   fun listAll(): List<T>
}

interface MutableConnectionRegistry<T : ConnectorConfiguration> : ConnectionRegistry<T> {
   fun register(targetPackage: PackageIdentifier, connectionConfiguration: T)
   fun remove(targetPackage: PackageIdentifier, connectionConfiguration: T) {
      return remove(targetPackage, connectionConfiguration.connectionName)
   }

   fun remove(targetPackage: PackageIdentifier, connectionName: String)
}

/**
 * An adaptor between the new ConfigFileConnectorsRegistry / Loader approach,
 * and the legacy XxxConnectionRegistry approach.
 *
 * SourceLoaderConnectorsRegistry supports reloading from sources like Schemas, and
 * is the preferred way for loading config.  However, ConnectionRegistry<> are everywhere.
 *
 * This lets us quickly adapt
 */
abstract class SourceLoaderConnectionRegistryAdapter<T : ConnectorConfiguration>(
   private val sourceLoaderConnectorsRegistry: SourceLoaderConnectorsRegistry,
   private val property: KProperty1<ConnectionsConfig, Map<String, T>>
) : MutableConnectionRegistry<T> {
   private fun getCurrent(): Map<String, T> {
      return property.invoke(sourceLoaderConnectorsRegistry.load())
   }

   override fun getConnection(name: String): T {
      return getCurrent().getValue(name)
   }

   override fun hasConnection(name: String): Boolean {
      return getCurrent().containsKey(name)
   }

   override fun listAll(): List<T> {
      return getCurrent().values.toList()
   }

   /**
    * Registers the config into the target map.
    * Will also invoke a writer, so the config is updated, and
    * the local cache in invalidated
    */
   override fun register(targetPackage: PackageIdentifier, connectionConfiguration: T) {
      // The full config, including all different types of connectors
      // but with env variables unresolved
      val currentConnectionsConfig = sourceLoaderConnectorsRegistry.loadUnresolvedConfig(targetPackage)

      val connectionAsHocon =
         connectionConfiguration.toConfig(name = configPath(connectionConfiguration.connectionName))
      val updated = ConfigFactory.empty()
         .withFallback(connectionAsHocon)
         .withFallback(currentConnectionsConfig)

      sourceLoaderConnectorsRegistry.saveConfig(targetPackage, updated)
   }

   private fun configPath(connectionName: String) = "${property.name}.$connectionName"

   override fun remove(targetPackage: PackageIdentifier, connectionName: String) {
      val currentConnectionsConfig = sourceLoaderConnectorsRegistry.loadUnresolvedConfig(targetPackage)
      val updated = currentConnectionsConfig.withoutPath(configPath(connectionName))

      sourceLoaderConnectorsRegistry.saveConfig(targetPackage, updated)

   }
}
