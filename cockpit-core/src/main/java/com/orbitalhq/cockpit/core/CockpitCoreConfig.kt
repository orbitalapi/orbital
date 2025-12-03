package com.orbitalhq.cockpit.core

import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.connectors.registry.RawConnectionsConnectorConfig
import com.orbitalhq.metrics.GaugeRegistry
import com.orbitalhq.nebula.NebulaSpringModule
import com.orbitalhq.schemaServer.core.config.ConfigHealthMonitor
import com.orbitalhq.schemas.readers.SourceConverterRegistry
import com.orbitalhq.schemas.readers.TaxiSourceConverter
import com.orbitalhq.spring.config.SourceLoaderServicesRegistry
import com.orbitalhq.spring.http.auth.schemes.HoconAuthTokensRepository
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

   @Bean
   fun hoconConfigHealthMonitor(
      servicesRegistry: SourceLoaderServicesRegistry,
      authRepository: HoconAuthTokensRepository,
      connectionsRepository: SourceLoaderConnectorsRegistry
   ): ConfigHealthMonitor {
      return ConfigHealthMonitor(listOf(servicesRegistry,authRepository,connectionsRepository))
   }
}


