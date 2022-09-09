package io.vyne.cask.observers

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.connections")
data class IngestionObserverConfigurationProperties(
   // Mutable to allow editing during testing.  Currently injected at runtime,
   // so no real mutation usecase.
   val kafka: MutableList<KafkaObserverConfiguration> = mutableListOf()
)

data class KafkaObserverConfiguration(val connectionName: String, val bootstrapServers: String, val topic: String)
