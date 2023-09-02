package com.orbitalhq.schema.spring.config.publisher

import com.orbitalhq.schema.publisher.SchemaPublisherService
import com.orbitalhq.schema.publisher.SchemaPublisherTransport
import com.orbitalhq.schema.spring.config.SchemaConfigProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import


@Configuration
@Import(
   HttpSchemaPublisherConfig::class,
   RSocketPublisherConfig::class
)
@EnableConfigurationProperties(
   SchemaPublisherConfigProperties::class,
   SchemaConfigProperties::class

)
class SchemaPublisherConfig {

   @Bean
   fun schemaPublisherService(
      @Value("\${spring.application.name:random.uuid}") publisherId: String,
      transport: SchemaPublisherTransport
   ) = SchemaPublisherService(publisherId, transport)
}
