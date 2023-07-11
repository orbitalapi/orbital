package io.vyne.cockpit.core.connectors

import io.vyne.config.FileConfigSourceLoader
import io.vyne.connectors.VyneConnectionsConfig
import io.vyne.connectors.config.ConfigFileConnectorsRegistry
import io.vyne.schema.consumer.SchemaConfigSourceLoader
import io.vyne.schema.consumer.SchemaStore
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(VyneConnectionsConfig::class)
class ConnectorsConfig {
   @Bean
   fun connectorsConfigRegistry(config: VyneConnectionsConfig, schemaStore: SchemaStore): ConfigFileConnectorsRegistry {
      return ConfigFileConnectorsRegistry(
         listOf(
            FileConfigSourceLoader(config.configFile),
            SchemaConfigSourceLoader(schemaStore, "connections.conf")
         )
      )
   }

}
