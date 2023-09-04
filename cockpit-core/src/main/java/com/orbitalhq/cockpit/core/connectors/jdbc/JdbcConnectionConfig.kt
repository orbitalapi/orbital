package com.orbitalhq.cockpit.core.connectors.jdbc

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory
import io.micrometer.core.instrument.MeterRegistry
import com.orbitalhq.connectors.VyneConnectionsConfig
import com.orbitalhq.connectors.jdbc.HikariJdbcConnectionFactory
import com.orbitalhq.connectors.jdbc.JdbcConnectionFactory
import com.orbitalhq.connectors.jdbc.JdbcInvoker
import com.orbitalhq.connectors.jdbc.registry.JdbcConfigFileConnectorRegistry
import com.orbitalhq.connectors.jdbc.registry.JdbcConnectionRegistry
import com.orbitalhq.schema.api.SchemaProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

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

