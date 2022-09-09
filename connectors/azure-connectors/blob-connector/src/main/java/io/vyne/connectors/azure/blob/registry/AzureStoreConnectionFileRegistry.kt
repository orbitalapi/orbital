package io.vyne.connectors.azure.blob.registry

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.vyne.connectors.registry.ConfigFileConnectorRegistry
import java.nio.file.Path

class AzureStoreConnectionFileRegistry(path: Path, fallback: Config = ConfigFactory.systemEnvironment()) :
   AzureStoreConnectionRegistry,
   ConfigFileConnectorRegistry<AzureStoreConnections, AzureStorageConnectorConfiguration>(
      path,
      fallback,
      AzureStoreConnections.CONFIG_PREFIX
   ) {

   override fun register(connectionConfiguration: AzureStorageConnectorConfiguration) {
      saveConnectorConfig(connectionConfiguration)
   }

   override fun remove(connectionConfiguration: AzureStorageConnectorConfiguration) {
      this.removeConnectorConfig(connectionConfiguration.connectionName)
   }

   override fun listAll(): List<AzureStorageConnectorConfiguration> = listConnections()

   override fun getConnectionMap(): Map<String, AzureStorageConnectorConfiguration> {
      return this.typedConfig().azureStores
   }

   override fun extract(config: Config): AzureStoreConnections = config.extract()

   override fun emptyConfig() = AzureStoreConnections()
}
