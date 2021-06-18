package io.vyne.cask.observers

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.connections")
data class IngestionObserverConfigurationProperties(val kafka: List<KafkaObserverConfiguration> = listOf())
data class KafkaObserverConfiguration(val connectionName: String, val bootstrapServers: String, val topic: String)
