package io.vyne.connectors.aws.core.registry

import io.vyne.PackageIdentifier
import io.vyne.connectors.config.aws.AwsConnectionConfiguration
import io.vyne.connectors.registry.MutableConnectionRegistry

class AwsInMemoryConnectionRegistry(configs: List<AwsConnectionConfiguration> = emptyList()) : AwsConnectionRegistry,
    MutableConnectionRegistry<AwsConnectionConfiguration> {
    private val connections: MutableMap<String, AwsConnectionConfiguration> =
        configs.associateBy { it.connectionName }.toMutableMap()

   fun register(connectionConfiguration: AwsConnectionConfiguration) {
      connections[connectionConfiguration.connectionName] = connectionConfiguration
   }

   override fun register(targetPackage: PackageIdentifier, connectionConfiguration: AwsConnectionConfiguration) {
      connections[connectionConfiguration.connectionName] = connectionConfiguration
   }

   override fun remove(targetPackage: PackageIdentifier, connectionConfiguration: AwsConnectionConfiguration) {
      super.remove(targetPackage, connectionConfiguration)
   }

   fun remove(connectionConfiguration: AwsConnectionConfiguration) {
        connections.remove(connectionConfiguration.connectionName)
    }

   override fun remove(targetPackage: PackageIdentifier, connectionName: String) {
      connections.remove(connectionName)
   }

    override fun hasConnection(name: String): Boolean {
        return connections.containsKey(name)
    }

    override fun getConnection(name: String): AwsConnectionConfiguration {
        return connections[name]!!
    }

    override fun listAll(): List<AwsConnectionConfiguration> {
        return connections.values.toList()
    }
}
