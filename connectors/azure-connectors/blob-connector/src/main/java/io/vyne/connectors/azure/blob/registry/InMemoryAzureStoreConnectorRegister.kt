package io.vyne.connectors.azure.blob.registry

import io.vyne.PackageIdentifier
import io.vyne.connectors.registry.MutableConnectionRegistry

class InMemoryAzureStoreConnectorRegister(configs: List<AzureStorageConnectorConfiguration> = emptyList()) :
    AzureStoreConnectionRegistry, MutableConnectionRegistry<AzureStorageConnectorConfiguration> {
   private val connections: MutableMap<String, AzureStorageConnectorConfiguration> =
      configs.associateBy { it.connectionName }.toMutableMap()

   override fun register(
      targetPackage: PackageIdentifier,
      connectionConfiguration: AzureStorageConnectorConfiguration
   ) {
      connections[connectionConfiguration.connectionName] = connectionConfiguration
   }

   override fun remove(targetPackage: PackageIdentifier, connectionName: String) {
      connections.remove(connectionName)
   }

   override fun hasConnection(name: String): Boolean = connections.containsKey(name)

   override fun getConnection(name: String): AzureStorageConnectorConfiguration =
      connections[name] ?: error("No JdbcConnection with name $name is registered")

   fun register(connectionConfiguration: AzureStorageConnectorConfiguration) {
      connections[connectionConfiguration.connectionName] = connectionConfiguration
   }

   fun remove(connectionConfiguration: AzureStorageConnectorConfiguration) {
      connections.remove(connectionConfiguration.connectionName)
   }

   override fun listAll(): List<AzureStorageConnectorConfiguration> {
      return this.connections.values.toList()
   }
   }
