package com.orbitalhq.connectors.kafka.registry

import com.orbitalhq.connectors.config.kafka.KafkaConnectionConfiguration
import com.orbitalhq.connectors.registry.ConnectionRegistry

interface KafkaConnectionRegistry : ConnectionRegistry<KafkaConnectionConfiguration> {
}
