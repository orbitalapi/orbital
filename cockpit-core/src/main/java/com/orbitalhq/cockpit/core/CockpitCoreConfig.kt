package com.orbitalhq.cockpit.core

import com.orbitalhq.connectors.registry.RawConnectionsConnectorConfig
import com.orbitalhq.metrics.GaugeRegistry
import com.orbitalhq.nebula.NebulaSpringModule
import com.orbitalhq.schemas.readers.SourceConverterRegistry
import com.orbitalhq.schemas.readers.TaxiSourceConverter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@ComponentScan
@Import(RawConnectionsConnectorConfig::class, NebulaSpringModule::class)
class CockpitCoreConfig {

   @Bean
   fun sourceConverterRegistry(): SourceConverterRegistry = SourceConverterRegistry(
      setOf(
         TaxiSourceConverter,
//         SoapWsdlSourceConverter,
      ),
      registerWithStaticRegistry = true
   )

   @Bean
   fun gaugeRegistry(metricsRegistry: MeterRegistry):GaugeRegistry = GaugeRegistry(metricsRegistry)
}


