package io.vyne.schema.spring.config.consumer

import io.vyne.schema.consumer.SchemaStore
import io.vyne.schema.consumer.StoreBackedSchemaProvider
import io.vyne.schema.spring.config.SchemaConfigProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(
   SchemaConsumerConfigProperties::class,
   SchemaConfigProperties::class
)
class SchemaConsumerConfig {
   @Bean
   fun schemaProvider(schemaStore: SchemaStore) = StoreBackedSchemaProvider(schemaStore)
}

