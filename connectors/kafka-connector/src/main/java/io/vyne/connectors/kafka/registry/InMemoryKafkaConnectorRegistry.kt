package io.vyne.connectors.kafka.registry

import io.vyne.connectors.config.kafka.KafkaConnectionConfiguration
import io.vyne.connectors.registry.MutableConnectionRegistry

class InMemoryKafkaConnectorRegistry(configs: List<KafkaConnectionConfiguration> = emptyList()) :
    KafkaConnectionRegistry, MutableConnectionRegistry<KafkaConnectionConfiguration> {


   private val connections: MutableMap<String, KafkaConnectionConfiguration> =
      configs.associateBy { it.connectionName }.toMutableMap()

   override fun hasConnection(name: String): Boolean = connections.containsKey(name)

   override fun getConnection(name: String): KafkaConnectionConfiguration =
      connections[name] ?: error("No JdbcConnection with name $name is registered")

   override fun register(connectionConfiguration: KafkaConnectionConfiguration) {
      connections[connectionConfiguration.connectionName] = connectionConfiguration
   }

   override fun remove(connectionConfiguration: KafkaConnectionConfiguration) {
      connections.remove(connectionConfiguration.connectionName)
   }

   override fun listAll(): List<KafkaConnectionConfiguration> {
      return this.connections.values.toList()
   }

}

