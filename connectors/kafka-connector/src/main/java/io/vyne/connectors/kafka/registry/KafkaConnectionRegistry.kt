package io.vyne.connectors.kafka.registry

import io.vyne.connectors.kafka.KafkaConnectionConfiguration
import io.vyne.connectors.registry.ConnectionRegistry

interface KafkaConnectionRegistry : ConnectionRegistry<KafkaConnectionConfiguration> {
}
