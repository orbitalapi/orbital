package io.vyne.connectors.jdbc.registry

import io.vyne.connectors.jdbc.JdbcConnectionConfiguration

interface JdbcConnectionRegistry {
   fun hasConnection(name: String): Boolean
   fun getConnection(name: String): JdbcConnectionConfiguration
   fun register(connectionConfiguration: JdbcConnectionConfiguration)
   fun remove(connectionConfiguration: JdbcConnectionConfiguration)
   fun listAll(): List<JdbcConnectionConfiguration>
}
