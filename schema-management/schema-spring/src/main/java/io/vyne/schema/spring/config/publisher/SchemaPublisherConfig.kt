package io.vyne.schema.spring.config.publisher

import io.vyne.schema.spring.config.SchemaConfigProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
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

}
