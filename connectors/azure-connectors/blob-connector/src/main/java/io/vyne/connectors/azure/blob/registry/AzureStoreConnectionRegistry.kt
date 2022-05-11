package io.vyne.connectors.azure.blob.registry

import io.vyne.connectors.registry.ConnectionConfigMap
import io.vyne.connectors.registry.ConnectionRegistry

interface AzureStoreConnectionRegistry: ConnectionRegistry<AzureStorageConnectorConfiguration>

data class AzureStoreConnections(
   val azureStores: MutableMap<String, AzureStorageConnectorConfiguration> = mutableMapOf()
) : ConnectionConfigMap {
   companion object {
      val CONFIG_PREFIX = AzureStoreConnections::azureStores.name  // must match the name of the param in the constructor
   }
}
