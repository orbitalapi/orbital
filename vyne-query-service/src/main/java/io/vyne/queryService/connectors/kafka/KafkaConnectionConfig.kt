package io.vyne.queryService.connectors.kafka

import io.vyne.connectors.VyneConnectionsConfig
import io.vyne.connectors.kafka.KafkaInvoker
import io.vyne.connectors.kafka.KafkaStreamManager
import io.vyne.connectors.kafka.registry.KafkaConfigFileConnectorRegistry
import io.vyne.connectors.kafka.registry.KafkaConnectionRegistry
import io.vyne.schema.api.SchemaProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(VyneConnectionsConfig::class)
class KafkaConnectionConfig {

   @Bean
   fun kafkaConnectionRegistry(config: VyneConnectionsConfig): KafkaConnectionRegistry {
      return KafkaConfigFileConnectorRegistry(config.configFile)
   }

   @Bean
   fun kafkaStreamManager(
      connectionRegistry: KafkaConnectionRegistry,
      schemaProvider: SchemaProvider
   ) = KafkaStreamManager(connectionRegistry, schemaProvider)

   @Bean
   fun kafkaInvoker(
      streamManager: KafkaStreamManager
   ): KafkaInvoker {
      return KafkaInvoker(
         streamManager
      )
   }
}
