package io.vyne.cockpit.core.connectors

import io.vyne.config.FileConfigSourceLoader
import io.vyne.connectors.VyneConnectionsConfig
import io.vyne.connectors.config.ConfigFileConnectorsRegistry
import io.vyne.schema.consumer.SchemaConfigSourceLoader
import io.vyne.schema.consumer.SchemaStore
import io.vyne.spring.config.EnvVariablesConfig
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(VyneConnectionsConfig::class, EnvVariablesConfig::class)
class ConnectorsConfig {
   @Bean
   fun connectorsConfigRegistry(
      config: VyneConnectionsConfig,
      schemaStore: SchemaStore,
      envVariablesConfig: EnvVariablesConfig
   ): ConfigFileConnectorsRegistry {
      return ConfigFileConnectorsRegistry(
         listOf(
            FileConfigSourceLoader(envVariablesConfig.envVariablesPath, failIfNotFound = false, packageIdentifier = EnvVariablesConfig.PACKAGE_IDENTIFIER),
            SchemaConfigSourceLoader(schemaStore, "env.conf"),
            FileConfigSourceLoader(config.configFile, packageIdentifier = VyneConnectionsConfig.PACKAGE_IDENTIFIER),
            SchemaConfigSourceLoader(schemaStore, "connections.conf")
         )
      )
   }

}
