package com.orbitalhq.connectors.azure.blob.registry

import com.orbitalhq.DefaultResultWithMessage
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.ResultWithMessage
import com.orbitalhq.connectors.registry.MutableConnectionRegistry

class InMemoryAzureStoreConnectorRegister(configs: List<AzureStorageConnectorConfiguration> = emptyList()) :
    AzureStoreConnectionRegistry, MutableConnectionRegistry<AzureStorageConnectorConfiguration> {
   private val connections: MutableMap<String, AzureStorageConnectorConfiguration> =
      configs.associateBy { it.connectionName }.toMutableMap()

   override fun register(
      targetPackage: PackageIdentifier,
      connectionConfiguration: AzureStorageConnectorConfiguration
   ): DefaultResultWithMessage {
      connections[connectionConfiguration.connectionName] = connectionConfiguration
      return ResultWithMessage.SUCCESS
   }

   override fun remove(targetPackage: PackageIdentifier, connectionName: String): DefaultResultWithMessage {
      connections.remove(connectionName)
      return ResultWithMessage.SUCCESS
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
