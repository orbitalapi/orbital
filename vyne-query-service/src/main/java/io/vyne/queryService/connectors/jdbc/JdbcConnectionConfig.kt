package io.vyne.queryService.connectors.jdbc

import io.vyne.connectors.jdbc.JdbcInvoker
import io.vyne.connectors.jdbc.registry.JdbcConfigFileConnectorRegistry
import io.vyne.connectors.jdbc.registry.JdbcConnectionRegistry
import io.vyne.schemaApi.SchemaProvider
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
   fun jdbcConnectionRegistry(config: VyneConnectionsConfig): JdbcConnectionRegistry {
      return JdbcConfigFileConnectorRegistry(config.configFile)
   }

   @Bean
   fun jdbcInvoker(
      connectionRegistry: JdbcConnectionRegistry,
      schemaProvider: SchemaProvider
   ): JdbcInvoker {
      return JdbcInvoker(
         connectionRegistry, schemaProvider
      )
   }
}

@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.connections")
data class VyneConnectionsConfig(
   val configFile: Path = Paths.get("config/connections.conf")
)
