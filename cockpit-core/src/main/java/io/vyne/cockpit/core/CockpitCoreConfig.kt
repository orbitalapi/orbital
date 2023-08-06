package io.vyne.cockpit.core

import io.vyne.connectors.registry.RawConnectionsConnectorConfig
import io.vyne.connectors.soap.SoapWsdlSourceConverter
import io.vyne.schemas.readers.SourceConverterRegistry
import io.vyne.schemas.readers.TaxiSourceConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@ComponentScan
@Import(RawConnectionsConnectorConfig::class)
class CockpitCoreConfig {


}
