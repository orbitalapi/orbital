package com.orbitalhq.schema.spring.config.consumer

import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schema.consumer.StoreBackedSchemaProvider
import com.orbitalhq.schema.spring.config.SchemaConfigProperties
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
   fun schemaProvider(schemaStore: SchemaStore): StoreBackedSchemaProvider {
      return StoreBackedSchemaProvider(schemaStore)
   }
}

