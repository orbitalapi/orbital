package io.vyne.cockpit.core

import io.vyne.auth.authentication.VyneUser
import io.vyne.connectors.registry.RawConnectionsConnectorConfig
import io.vyne.connectors.soap.SoapWsdlSourceConverter
import io.vyne.schemas.readers.SourceConverterRegistry
import io.vyne.schemas.readers.TaxiSourceConverter
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
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


}
