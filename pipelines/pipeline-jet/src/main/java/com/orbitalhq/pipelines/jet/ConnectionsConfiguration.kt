package com.orbitalhq.pipelines.jet

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory
import io.micrometer.core.instrument.MeterRegistry
import com.orbitalhq.config.FileConfigSourceLoader
import com.orbitalhq.connectors.VyneConnectionsConfig
import com.orbitalhq.connectors.aws.core.registry.AwsConfigFileConnectionRegistry
import com.orbitalhq.connectors.aws.core.registry.AwsConnectionRegistry
import com.orbitalhq.connectors.aws.core.registry.SourceLoaderAwsConnectionRegistry
import com.orbitalhq.connectors.azure.blob.registry.AzureStoreConnectionFileRegistry
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.connectors.jdbc.HikariJdbcConnectionFactory
import com.orbitalhq.connectors.jdbc.JdbcConnectionFactory
import com.orbitalhq.connectors.jdbc.registry.JdbcConnectionRegistry
import com.orbitalhq.connectors.jdbc.registry.SourceLoaderJdbcConnectionRegistry
import com.orbitalhq.connectors.kafka.registry.KafkaConfigFileConnectorRegistry
import com.orbitalhq.connectors.kafka.registry.KafkaConnectionRegistry
import com.orbitalhq.connectors.kafka.registry.SourceLoaderKafkaConnectionRegistry
import com.orbitalhq.schema.consumer.SchemaConfigSourceLoader
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.spring.config.EnvVariablesConfig
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(VyneConnectionsConfig::class, EnvVariablesConfig::class)
class ConnectionsConfiguration {

   @Bean
   fun configFileConnectorsRegistry(
      config: VyneConnectionsConfig,
      schemaStore: SchemaStore,
      envVariablesConfig: EnvVariablesConfig
   ): SourceLoaderConnectorsRegistry {
       return SourceLoaderConnectorsRegistry(
         listOf(
            FileConfigSourceLoader(envVariablesConfig.envVariablesPath, failIfNotFound = false, packageIdentifier = EnvVariablesConfig.PACKAGE_IDENTIFIER),
            SchemaConfigSourceLoader(schemaStore, "env.conf"),
            FileConfigSourceLoader(
               config.configFile,
               packageIdentifier = VyneConnectionsConfig.PACKAGE_IDENTIFIER,
               failIfNotFound = false
            ),
            SchemaConfigSourceLoader(schemaStore, "connections.conf")
         )
      )
   }

   @Bean
   fun jdbcConnectionRegistry(config: SourceLoaderConnectorsRegistry): JdbcConnectionRegistry {
       return SourceLoaderJdbcConnectionRegistry(config)
   }

   @Bean
   fun awsConnectionRegistry(sourceLoaderConnectorsRegistry: SourceLoaderConnectorsRegistry): AwsConnectionRegistry {
      return SourceLoaderAwsConnectionRegistry(sourceLoaderConnectorsRegistry)
   }

   @Bean
   fun kafkaConnectionRegistry(sourceLoaderConnectorsRegistry: SourceLoaderConnectorsRegistry): KafkaConnectionRegistry {
      return SourceLoaderKafkaConnectionRegistry(sourceLoaderConnectorsRegistry)
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


