package io.vyne.connectors.jdbc.registry

import io.vyne.connectors.jdbc.JdbcConnectionConfiguration
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import javax.sql.DataSource


class InMemoryJdbcConnectionRegistry(configs: List<JdbcConnectionConfiguration> = emptyList()) :
   JdbcConnectionRegistry {
   private val connections: MutableMap<String, JdbcConnectionConfiguration> =
      configs.associateBy { it.connectionName }.toMutableMap()

   override fun hasConnection(name: String): Boolean = connections.containsKey(name)

   override fun getConnection(name: String): JdbcConnectionConfiguration =
      connections[name] ?: error("No JdbcConnection with name $name is registered")

   override fun register(connectionConfiguration: JdbcConnectionConfiguration) {
      connections[connectionConfiguration.connectionName] = connectionConfiguration
   }

   override fun remove(connectionConfiguration: JdbcConnectionConfiguration) {
      connections.remove(connectionConfiguration.connectionName)
   }

   override fun listAll(): List<JdbcConnectionConfiguration> {
      return this.connections.values.toList()
   }
}


