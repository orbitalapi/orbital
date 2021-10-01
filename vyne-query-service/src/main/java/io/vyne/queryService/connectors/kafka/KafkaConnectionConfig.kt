package io.vyne.queryService.connectors.kafka

import io.vyne.connectors.kafka.KafkaInvoker
import io.vyne.connectors.kafka.registry.KafkaConfigFileConnectorRegistry
import io.vyne.connectors.kafka.registry.KafkaConnectionRegistry
import io.vyne.queryService.connectors.jdbc.VyneConnectionsConfig
import io.vyne.schemaStore.SchemaProvider
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
   fun kafkaInvoker(
      connectionRegistry: KafkaConnectionRegistry,
      schemaProvider: SchemaProvider
   ): KafkaInvoker {
      return KafkaInvoker(
         connectionRegistry,
         schemaProvider
      )
   }
}
