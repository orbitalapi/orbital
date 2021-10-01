package io.vyne.connectors.kafka.registry

import io.vyne.connectors.kafka.KafkaConnectionConfiguration

interface KafkaConnectionRegistry {
   fun hasConnection(name: String): Boolean
   fun getConnection(name: String): KafkaConnectionConfiguration
   fun register(connectionConfiguration: KafkaConnectionConfiguration)
   fun remove(connectionConfiguration: KafkaConnectionConfiguration)
   fun listAll(): List<KafkaConnectionConfiguration>
}
