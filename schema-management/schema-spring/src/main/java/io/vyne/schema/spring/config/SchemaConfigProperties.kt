package io.vyne.schema.spring.config

import io.vyne.schema.api.VyneSchemaInteractionMethod
import io.vyne.schema.spring.config.consumer.SchemaConsumerConfigProperties
import io.vyne.schema.spring.config.publisher.SchemaPublisherConfigProperties
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = SchemaConfigProperties.SCHEMA_CONFIG)
data class SchemaConfigProperties(
   // By default, use the application name from Spring, and allow Spring's
   // Discovery Client to resolve.  For clients using their own discovery
   // (eg., network DNS via k8s or docker), they can provide an alternative here.
   val schemaServerAddress: String = "schema-server",

   // Optional if not using rsocket.
   // Since we expect the same config for both producer and consumer
   // (and wiring up separate rsocket factories isn't something we want to do),
   // the rsocket port lives at this level
   val schemaServerRSocketPort: Int = 7655,
   val publisher: SchemaPublisherConfigProperties? = null,
   val consumer: SchemaConsumerConfigProperties? = null
) {
   companion object {
      const val SCHEMA_CONFIG = "vyne.schema"
   }

   val schemaServerAddressType: AddressType
      get() = if (schemaServerAddress.startsWith("http") || schemaServerAddress.startsWith("https")) {
         AddressType.URL
      } else {
         AddressType.DISCOVERY_CLIENT_REFERENCE
      }

   enum class AddressType {
      URL,
      DISCOVERY_CLIENT_REFERENCE
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

