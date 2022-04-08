package io.vyne.queryService.connectors.jdbc

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory
import io.micrometer.core.instrument.MeterRegistry
import io.vyne.connectors.VyneConnectionsConfig
import io.vyne.connectors.jdbc.HikariJdbcConnectionFactory
import io.vyne.connectors.jdbc.JdbcConnectionFactory
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
   fun jdbcConnectionFactory(
      connectionRegistry: JdbcConnectionRegistry,
      hikariConfig: HikariConfig,
      meter: MeterRegistry
   ): JdbcConnectionFactory {
      return HikariJdbcConnectionFactory(connectionRegistry, hikariConfig, MicrometerMetricsTrackerFactory(meter))
   }

   @Bean
   fun jdbcInvoker(
      connectionFactory: JdbcConnectionFactory,
      schemaProvider: SchemaProvider
   ): JdbcInvoker {
      return JdbcInvoker(
         connectionFactory, schemaProvider
      )
   }
}

