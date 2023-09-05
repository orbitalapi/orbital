package com.orbitalhq.cockpit.core

import com.orbitalhq.connectors.registry.RawConnectionsConnectorConfig
import com.orbitalhq.connectors.soap.SoapWsdlSourceConverter
import com.orbitalhq.schemas.readers.SourceConverterRegistry
import com.orbitalhq.schemas.readers.TaxiSourceConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@ComponentScan
@Import(RawConnectionsConnectorConfig::class)
class CockpitCoreConfig {

   @Bean
   fun sourceConverterRegistry(): SourceConverterRegistry = SourceConverterRegistry(
      setOf(
         TaxiSourceConverter,
         SoapWsdlSourceConverter
      ),
      registerWithStaticRegistry = true
   )
}


