package io.vyne.connectors.aws.core.registry

import io.vyne.connectors.aws.core.AwsConnectionConfiguration

class AwsInMemoryConnectionRegistry(configs: List<AwsConnectionConfiguration> = emptyList()) : AwsConnectionRegistry {
   private val connections: MutableMap<String, AwsConnectionConfiguration> =
      configs.associateBy { it.connectionName }.toMutableMap()

   override fun hasConnection(name: String): Boolean {
      return connections.containsKey(name)
   }

   override fun getConnection(name: String): AwsConnectionConfiguration {
      return connections[name]!!
   }

   override fun register(connectionConfiguration: AwsConnectionConfiguration) {
      connections[connectionConfiguration.connectionName] = connectionConfiguration
   }

   override fun remove(connectionConfiguration: AwsConnectionConfiguration) {
      connections.remove(connectionConfiguration.connectionName)
   }

   override fun listAll(): List<AwsConnectionConfiguration> {
      return connections.values.toList()
   }
}
