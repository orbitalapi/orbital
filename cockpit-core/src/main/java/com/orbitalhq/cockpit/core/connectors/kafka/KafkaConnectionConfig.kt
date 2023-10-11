package com.orbitalhq.cockpit.core.connectors.kafka

import com.orbitalhq.connectors.VyneConnectionsConfig
import com.orbitalhq.connectors.kafka.KafkaInvoker
import com.orbitalhq.connectors.kafka.KafkaStreamManager
import com.orbitalhq.connectors.kafka.registry.KafkaConfigFileConnectorRegistry
import com.orbitalhq.connectors.kafka.registry.KafkaConnectionRegistry
import com.orbitalhq.schema.api.SchemaProvider
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
