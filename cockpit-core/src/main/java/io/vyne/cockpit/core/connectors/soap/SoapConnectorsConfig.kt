package io.vyne.cockpit.core.connectors.soap

import io.vyne.connectors.soap.SoapClientCache
import io.vyne.connectors.soap.SoapInvoker
import io.vyne.schema.api.SchemaProvider
import io.vyne.schema.consumer.SchemaChangedEventProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SoapConnectorsConfig {

   @Bean
   fun soapInvoker(
      schemaProvider: SchemaProvider,
      schemaChangedEventProvider: SchemaChangedEventProvider
   ): SoapInvoker {
      return SoapInvoker(
         schemaProvider,
         SoapClientCache(
            schemaChangedEventProvider = schemaChangedEventProvider
         )
      )
   }
}
