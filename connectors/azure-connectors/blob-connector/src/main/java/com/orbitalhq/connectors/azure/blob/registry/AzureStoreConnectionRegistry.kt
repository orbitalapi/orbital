package com.orbitalhq.connectors.azure.blob.registry

import com.orbitalhq.connectors.registry.ConnectionConfigMap
import com.orbitalhq.connectors.registry.ConnectionRegistry

interface AzureStoreConnectionRegistry: ConnectionRegistry<AzureStorageConnectorConfiguration>

data class AzureStoreConnections(
   val azureStores: MutableMap<String, AzureStorageConnectorConfiguration> = mutableMapOf()
) : ConnectionConfigMap {
   companion object {
      val CONFIG_PREFIX = AzureStoreConnections::azureStores.name  // must match the name of the param in the constructor
   }
}
