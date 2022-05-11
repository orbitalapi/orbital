package io.vyne.connectors.registry

interface ConnectionRegistry<T : ConnectorConfiguration> {
   fun hasConnection(name: String): Boolean
   fun getConnection(name: String): T
   fun register(connectionConfiguration: T)
   fun remove(connectionConfiguration: T)
   fun listAll(): List<T>
}
