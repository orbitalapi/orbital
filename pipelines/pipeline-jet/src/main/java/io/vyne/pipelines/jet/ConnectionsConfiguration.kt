package io.vyne.pipelines.jet

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory
import io.micrometer.core.instrument.MeterRegistry
import io.vyne.connectors.VyneConnectionsConfig
import io.vyne.connectors.aws.core.registry.AwsConfigFileConnectionRegistry
import io.vyne.connectors.azure.blob.registry.AzureStoreConnectionFileRegistry
import io.vyne.connectors.jdbc.HikariJdbcConnectionFactory
import io.vyne.connectors.jdbc.JdbcConnectionFactory
import io.vyne.connectors.jdbc.registry.JdbcConfigFileConnectorRegistry
import io.vyne.connectors.jdbc.registry.JdbcConnectionRegistry
import io.vyne.connectors.kafka.registry.KafkaConfigFileConnectorRegistry
import io.vyne.connectors.kafka.registry.KafkaConnectionRegistry
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path
import java.nio.file.Paths

@Configuration
@EnableConfigurationProperties(VyneConnectionsConfig::class)
class ConnectionsConfiguration {
   @Bean
   fun jdbcConnectionRegistry(config: VyneConnectionsConfig): JdbcConnectionRegistry {
      return JdbcConfigFileConnectorRegistry(config.configFile)
   }

   @Bean
   fun awsConnectionRegistry(config: VyneConnectionsConfig): AwsConfigFileConnectionRegistry {
      return AwsConfigFileConnectionRegistry(config.configFile)
   }

   @Bean
   fun kafkaConnectionRegistry(config: VyneConnectionsConfig): KafkaConnectionRegistry {
      return KafkaConfigFileConnectorRegistry(config.configFile)
   }

   @Bean
   fun hikariConfig(): HikariConfig {
      return HikariConfig()
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
   fun azureStoreConnectionRegistry(config: VyneConnectionsConfig): AzureStoreConnectionFileRegistry {
      return AzureStoreConnectionFileRegistry(config.configFile)
   }
}


