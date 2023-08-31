package io.vyne.connectors.azure.blob.registry

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.vyne.PackageIdentifier
import io.vyne.connectors.registry.ConfigFileConnectorRegistry
import io.vyne.connectors.registry.MutableConnectionRegistry
import java.nio.file.Path

/**
 * This approach needs updating:
 *  - Connection should be for Azure, not AzureBlobStore.  Move BlobStore related things to the invoker and annotation.
 *  - Implement a SourceLoaderConnectionRegistry, which supports loading connection config from Schemas,etc.,  See SourceLoaderAwsConnectionRegistry
 *  - Seperate read and write operations - favour not implementing the Mutable methods if possible
 */
@Deprecated("this needs updating.")
class AzureStoreConnectionFileRegistry(path: Path, fallback: Config = ConfigFactory.systemEnvironment()) :
    AzureStoreConnectionRegistry, MutableConnectionRegistry<AzureStorageConnectorConfiguration>,
   ConfigFileConnectorRegistry<AzureStoreConnections, AzureStorageConnectorConfiguration>(
      path,
      fallback,
      AzureStoreConnections.CONFIG_PREFIX
   ) {
   override fun register(
      targetPackage: PackageIdentifier,
      connectionConfiguration: AzureStorageConnectorConfiguration
   ) {
      TODO("Not yet implemented")
   }

   override fun remove(targetPackage: PackageIdentifier, connectionName: String) {
      TODO("Not yet implemented")
   }

//   override fun register(connectionConfiguration: AzureStorageConnectorConfiguration) {
//      saveConnectorConfig(connectionConfiguration)
//   }
//
//   override fun remove(connectionConfiguration: AzureStorageConnectorConfiguration) {
//      this.removeConnectorConfig(connectionConfiguration.connectionName)
//   }

   override fun listAll(): List<AzureStorageConnectorConfiguration> = listConnections()

   override fun getConnectionMap(): Map<String, AzureStorageConnectorConfiguration> {
      return this.typedConfig().azureStores
   }

   override fun extract(config: Config): AzureStoreConnections = config.extract()

   override fun emptyConfig() = AzureStoreConnections()
}
