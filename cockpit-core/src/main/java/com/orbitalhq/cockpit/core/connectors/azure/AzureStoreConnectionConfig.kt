package com.orbitalhq.cockpit.core.connectors.azure

import com.orbitalhq.connectors.VyneConnectionsConfig
import com.orbitalhq.connectors.azure.blob.AzureStreamProvider
import com.orbitalhq.connectors.azure.blob.StoreInvoker
import com.orbitalhq.connectors.azure.blob.registry.AzureStoreConnectionFileRegistry
import com.orbitalhq.schema.api.SchemaProvider
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
   fun azureStoreInvoker(
      schemaProvider: SchemaProvider,
      azureConnectionRegistry: AzureStoreConnectionFileRegistry
   ): StoreInvoker {
      return StoreInvoker(AzureStreamProvider(), azureConnectionRegistry, schemaProvider)
   }
}
