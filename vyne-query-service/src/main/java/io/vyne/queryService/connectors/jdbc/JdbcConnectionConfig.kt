package io.vyne.queryService.connectors.jdbc

import io.vyne.connectors.jdbc.registry.JdbcConfigFileConnectorRegistry
import io.vyne.connectors.jdbc.registry.JdbcConnectionRegistry
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path
import java.nio.file.Paths

@Configuration
@EnableConfigurationProperties(VyneConnectionsConfig::class)
class JdbcConnectionConfig {

   @Bean
   fun connectionRegistry(config: VyneConnectionsConfig): JdbcConnectionRegistry {
      return JdbcConfigFileConnectorRegistry(config.configFile)
   }
}

@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.connections")
data class VyneConnectionsConfig(
   val configFile: Path = Paths.get("connections.conf")
)
