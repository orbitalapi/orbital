package io.vyne.cockpit.core

import io.vyne.connectors.registry.RawConnectionsConnectorConfig
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@ComponentScan
@Import(RawConnectionsConnectorConfig::class)
class CockpitCoreConfig {
}
