package com.orbitalhq.schema.spring.config

import com.orbitalhq.schema.api.VyneSchemaInteractionMethod
import com.orbitalhq.schema.spring.config.consumer.SchemaConsumerConfigProperties
import com.orbitalhq.schema.spring.config.publisher.SchemaPublisherConfigProperties
import org.springframework.boot.context.properties.ConfigurationProperties

//@ConstructorBinding
@ConfigurationProperties(prefix = SchemaConfigProperties.SCHEMA_CONFIG)
data class SchemaConfigProperties(
   val publisher: SchemaPublisherConfigProperties? = null,
   val consumer: SchemaConsumerConfigProperties? = null
) {
   companion object {
      const val SCHEMA_CONFIG = "vyne.schema"
   }
}


/**
 * Interface to enforce consistency across shared
 * config settings in producer / consumer config param types
 */
interface SchemaTransportConfigProperties {
   val method: VyneSchemaInteractionMethod
      get() = VyneSchemaInteractionMethod.RSocket

}

