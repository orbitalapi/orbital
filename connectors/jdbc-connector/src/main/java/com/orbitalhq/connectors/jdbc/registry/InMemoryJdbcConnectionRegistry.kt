package com.orbitalhq.connectors.jdbc.registry

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.connectors.config.jdbc.JdbcConnectionConfiguration
import com.orbitalhq.connectors.registry.MutableConnectionRegistry


class InMemoryJdbcConnectionRegistry(configs: List<JdbcConnectionConfiguration> = emptyList()) :
   JdbcConnectionRegistry, MutableConnectionRegistry<JdbcConnectionConfiguration> {
   private val connections: MutableMap<String, JdbcConnectionConfiguration> =
      configs.associateBy { it.connectionName }.toMutableMap()

   override fun hasConnection(name: String): Boolean = connections.containsKey(name)

   override fun getConnection(name: String): JdbcConnectionConfiguration =
      connections[name] ?: error("No JdbcConnection with name $name is registered")

   fun register(connectionConfiguration: JdbcConnectionConfiguration) {
      connections[connectionConfiguration.connectionName] = connectionConfiguration
   }

   override fun register(targetPackage: PackageIdentifier, connectionConfiguration: JdbcConnectionConfiguration) {
      connections[connectionConfiguration.connectionName] = connectionConfiguration
   }

   override fun remove(targetPackage: PackageIdentifier, connectionConfiguration: JdbcConnectionConfiguration) {
      connections.remove(connectionConfiguration.connectionName)
   }

   override fun remove(targetPackage: PackageIdentifier, connectionName: String) {
      connections.remove(connectionName)
   }

   override fun listAll(): List<JdbcConnectionConfiguration> {
      return this.connections.values.toList()
   }
}


