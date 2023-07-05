package io.vyne.pipelines.jet

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory
import io.micrometer.core.instrument.MeterRegistry
import io.vyne.config.FileHoconLoader
import io.vyne.connectors.VyneConnectionsConfig
import io.vyne.connectors.aws.core.registry.AwsConfigFileConnectionRegistry
import io.vyne.connectors.azure.blob.registry.AzureStoreConnectionFileRegistry
import io.vyne.connectors.config.ConfigFileConnectorsRegistry
import io.vyne.connectors.jdbc.HikariJdbcConnectionFactory
import io.vyne.connectors.jdbc.JdbcConnectionFactory
import io.vyne.connectors.jdbc.registry.JdbcConnectionRegistry
import io.vyne.connectors.jdbc.registry.ReloadingJdbcConnectionRegistry
import io.vyne.connectors.kafka.registry.KafkaConfigFileConnectorRegistry
import io.vyne.connectors.kafka.registry.KafkaConnectionRegistry
import io.vyne.schema.consumer.SchemaHoconLoader
import io.vyne.schema.consumer.SchemaStore
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(VyneConnectionsConfig::class)
class ConnectionsConfiguration {

   @Bean
   fun configFileConnectorsRegistry(
      config: VyneConnectionsConfig,
      schemaStore: SchemaStore
   ): ConfigFileConnectorsRegistry {
      return ConfigFileConnectorsRegistry(
         listOf(
            FileHoconLoader(config.configFile),
            SchemaHoconLoader(schemaStore, "connections.conf")
         )
      )
   }

   @Bean
   fun jdbcConnectionRegistry(config: ConfigFileConnectorsRegistry): JdbcConnectionRegistry {
      return ReloadingJdbcConnectionRegistry(config)
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


