package io.vyne.schema.spring.config.consumer

import io.vyne.schema.api.VyneSchemaInteractionMethod
import io.vyne.schema.spring.config.SchemaConfigProperties.Companion.SCHEMA_CONFIG
import io.vyne.schema.spring.config.SchemaTransportConfigProperties
import io.vyne.schema.spring.config.consumer.SchemaConsumerConfigProperties.Companion.CONSUMER_CONFIG
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

@ConfigurationProperties(prefix = CONSUMER_CONFIG)
data class SchemaConsumerConfigProperties(
   override val method: VyneSchemaInteractionMethod = VyneSchemaInteractionMethod.RSocket,
) : SchemaTransportConfigProperties {
   companion object {
      const val CONSUMER_CONFIG = "$SCHEMA_CONFIG.consumer"
      const val CONSUMER_METHOD = "$CONSUMER_CONFIG.method"
   }
}

