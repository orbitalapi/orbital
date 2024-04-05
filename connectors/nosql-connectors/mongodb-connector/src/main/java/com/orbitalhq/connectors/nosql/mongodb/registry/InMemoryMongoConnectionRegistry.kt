package com.orbitalhq.connectors.nosql.mongodb.registry

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.connectors.config.mongodb.MongoConnectionConfiguration
import com.orbitalhq.connectors.registry.MutableConnectionRegistry

class InMemoryMongoConnectionRegistry(configs: List<MongoConnectionConfiguration> = emptyList()) : MongoConnectionRegistry,
   MutableConnectionRegistry<MongoConnectionConfiguration> {
   private val connections: MutableMap<String, MongoConnectionConfiguration> =
      configs.associateBy { it.connectionName }.toMutableMap()

   override fun register(targetPackage: PackageIdentifier, connectionConfiguration: MongoConnectionConfiguration) {
      connections[connectionConfiguration.connectionName] = connectionConfiguration
   }

   override fun remove(targetPackage: PackageIdentifier, connectionName: String) {
      connections.remove(connectionName)
   }

   override fun hasConnection(name: String): Boolean = connections.containsKey(name)

   override fun getConnection(name: String): MongoConnectionConfiguration = connections[name] ?: error("No Mongo Connection with name $name is registered")

   override fun listAll(): List<MongoConnectionConfiguration> = this.connections.values.toList()

}
