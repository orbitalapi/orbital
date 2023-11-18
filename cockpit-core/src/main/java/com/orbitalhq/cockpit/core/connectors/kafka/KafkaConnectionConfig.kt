package com.orbitalhq.cockpit.core.connectors.kafka

import com.orbitalhq.connectors.VyneConnectionsConfig
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.connectors.kafka.KafkaInvoker
import com.orbitalhq.connectors.kafka.KafkaStreamManager
import com.orbitalhq.connectors.kafka.KafkaStreamPublisher
import com.orbitalhq.connectors.kafka.registry.KafkaConnectionRegistry
import com.orbitalhq.connectors.kafka.registry.SourceLoaderKafkaConnectionRegistry
import com.orbitalhq.models.format.FormatRegistry
import com.orbitalhq.schema.api.SchemaProvider
import io.micrometer.core.instrument.MeterRegistry
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
      schemaProvider: SchemaProvider,
      formatRegistry: FormatRegistry,
      meterRegistry: MeterRegistry
   ) = KafkaStreamManager(connectionRegistry, schemaProvider, formatRegistry = formatRegistry, meterRegistry = meterRegistry)

   @Bean
   fun kafkaStreamPublisher(
      connectionRegistry: KafkaConnectionRegistry,
      formatRegistry: FormatRegistry,
      meterRegistry: MeterRegistry
   ): KafkaStreamPublisher {
      return KafkaStreamPublisher(connectionRegistry, formatRegistry = formatRegistry, meterRegistry = meterRegistry)
   }

   @Bean
   fun kafkaInvoker(
      streamManager: KafkaStreamManager,
      streamPublisher: KafkaStreamPublisher
   ): KafkaInvoker {
      return KafkaInvoker(
         streamManager,
         streamPublisher
      )
   }
}
