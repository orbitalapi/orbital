package com.orbitalhq.cockpit.core

import com.orbitalhq.auth.authentication.VyneUser
import com.orbitalhq.connectors.registry.RawConnectionsConnectorConfig
import com.orbitalhq.connectors.soap.SoapWsdlSourceConverter
import com.orbitalhq.schemas.readers.SourceConverterRegistry
import com.orbitalhq.schemas.readers.TaxiSourceConverter
import org.springframework.context.annotation.Bean
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@ComponentScan
@EnableJpaRepositories
@EntityScan(basePackageClasses = [VyneUser::class])
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
