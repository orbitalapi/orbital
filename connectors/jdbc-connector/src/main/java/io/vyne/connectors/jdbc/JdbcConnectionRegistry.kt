package io.vyne.connectors.jdbc

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

data class JdbcConnection(val name: String, val template: NamedParameterJdbcTemplate)
class JdbcConnectionRegistry(connections: List<JdbcConnection>) {
   private val connections = connections.associateBy { it.name }

   fun hasConnection(name: String): Boolean = connections.containsKey(name)

   fun getConnection(name: String): JdbcConnection =
      connections[name] ?: error("No JdbcConnection with name $name is registered")
}
