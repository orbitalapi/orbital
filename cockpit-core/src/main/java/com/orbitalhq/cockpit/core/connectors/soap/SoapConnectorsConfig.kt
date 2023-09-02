package com.orbitalhq.cockpit.core.connectors.soap

import com.orbitalhq.connectors.soap.SoapClientCache
import com.orbitalhq.connectors.soap.SoapInvoker
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schema.consumer.SchemaChangedEventProvider
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
