package com.orbitalhq.cockpit.core.connectors.kafka

import com.orbitalhq.connectors.VyneConnectionsConfig
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.connectors.kafka.KafkaInvoker
import com.orbitalhq.connectors.kafka.KafkaStreamManager
import com.orbitalhq.connectors.kafka.registry.KafkaConnectionRegistry
import com.orbitalhq.connectors.kafka.registry.SourceLoaderKafkaConnectionRegistry
import com.orbitalhq.schema.api.SchemaProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(VyneConnectionsConfig::class)
class KafkaConnectionConfig {

   @Bean
   fun kafkaConnectionRegistry(sourceLoaderConnectorsRegistry: SourceLoaderConnectorsRegistry): KafkaConnectionRegistry {
      return SourceLoaderKafkaConnectionRegistry(sourceLoaderConnectorsRegistry)
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
