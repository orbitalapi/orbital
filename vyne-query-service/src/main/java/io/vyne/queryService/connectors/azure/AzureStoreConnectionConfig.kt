package io.vyne.queryService.connectors.azure

import io.vyne.connectors.azure.blob.AzureStreamProvider
import io.vyne.connectors.azure.blob.StoreInvoker
import io.vyne.connectors.azure.blob.registry.AzureStoreConnectionFileRegistry
import io.vyne.queryService.connectors.jdbc.VyneConnectionsConfig
import io.vyne.schema.api.SchemaProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
@EnableConfigurationProperties(VyneConnectionsConfig::class)
class AzureStoreConnectionConfig {
   @Bean
   fun azureStoreConnectionRegistry(config: VyneConnectionsConfig): AzureStoreConnectionFileRegistry {
      return AzureStoreConnectionFileRegistry(config.configFile)
   }

   @Bean
   fun azureStoreInvoker(schemaProvider: SchemaProvider, azureConnectionRegistry: AzureStoreConnectionFileRegistry): StoreInvoker {
      return StoreInvoker(AzureStreamProvider(), azureConnectionRegistry, schemaProvider)
   }
}
