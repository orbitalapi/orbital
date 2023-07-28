package io.vyne.connectors.kafka.registry

import io.vyne.connectors.config.kafka.KafkaConnectionConfiguration
import io.vyne.connectors.registry.ConnectionRegistry

interface KafkaConnectionRegistry : ConnectionRegistry<KafkaConnectionConfiguration> {
}
