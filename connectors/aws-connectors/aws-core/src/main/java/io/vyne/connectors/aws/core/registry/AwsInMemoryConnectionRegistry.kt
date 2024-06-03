package com.orbitalhq.connectors.aws.core.registry

import com.orbitalhq.DefaultResultWithMessage
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.ResultWithMessage
import com.orbitalhq.connectors.config.aws.AwsConnectionConfiguration
import com.orbitalhq.connectors.registry.MutableConnectionRegistry

class AwsInMemoryConnectionRegistry(configs: List<AwsConnectionConfiguration> = emptyList()) : AwsConnectionRegistry,
    MutableConnectionRegistry<AwsConnectionConfiguration> {
    private val connections: MutableMap<String, AwsConnectionConfiguration> =
        configs.associateBy { it.connectionName }.toMutableMap()

   fun register(connectionConfiguration: AwsConnectionConfiguration) {
      connections[connectionConfiguration.connectionName] = connectionConfiguration
   }

   override fun register(targetPackage: PackageIdentifier, connectionConfiguration: AwsConnectionConfiguration): DefaultResultWithMessage {
      connections[connectionConfiguration.connectionName] = connectionConfiguration
      return ResultWithMessage.SUCCESS
   }

   override fun remove(targetPackage: PackageIdentifier, connectionConfiguration: AwsConnectionConfiguration): DefaultResultWithMessage {
      super.remove(targetPackage, connectionConfiguration)
      return ResultWithMessage.SUCCESS
   }

   fun remove(connectionConfiguration: AwsConnectionConfiguration) {
        connections.remove(connectionConfiguration.connectionName)
    }

   override fun remove(targetPackage: PackageIdentifier, connectionName: String): DefaultResultWithMessage {
      connections.remove(connectionName)
      return ResultWithMessage.SUCCESS
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
