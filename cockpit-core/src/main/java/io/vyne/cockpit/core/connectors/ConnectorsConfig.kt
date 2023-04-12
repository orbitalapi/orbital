package io.vyne.cockpit.core.connectors

import io.vyne.connectors.VyneConnectionsConfig
import io.vyne.connectors.config.ConfigFileConnectorsRegistry
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(VyneConnectionsConfig::class)
class ConnectorsConfig {
   @Bean
   fun connectorsConfigRegistry(config: VyneConnectionsConfig): ConfigFileConnectorsRegistry {
      return ConfigFileConnectorsRegistry(config.configFile)
   }

}
