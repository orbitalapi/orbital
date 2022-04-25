package io.vyne.schema.spring.config.publisher

import io.vyne.schema.api.VyneSchemaInteractionMethod
import io.vyne.schema.spring.config.SchemaConfigProperties.Companion.SCHEMA_CONFIG
import io.vyne.schema.spring.config.SchemaTransportConfigProperties
import io.vyne.schema.spring.config.publisher.SchemaPublisherConfigProperties.Companion.PUBLISHER_CONFIG
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

@ConfigurationProperties(prefix = PUBLISHER_CONFIG)
data class SchemaPublisherConfigProperties(
   override val method: VyneSchemaInteractionMethod = VyneSchemaInteractionMethod.RSocket,
   @NestedConfigurationProperty
   val http: HttpPublisherConfigParams = HttpPublisherConfigParams(),
) : SchemaTransportConfigProperties {
   companion object {
      const val PUBLISHER_CONFIG = "$SCHEMA_CONFIG.publisher"
      const val PUBLISHER_METHOD = "$PUBLISHER_CONFIG.method"
   }
}
