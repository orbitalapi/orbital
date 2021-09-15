package io.vyne.connectors.jdbc

import io.vyne.connectors.Connector
import io.vyne.connectors.ConnectorType
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

/**
 * Really just used in tests, where we already have a JdbcTemplate.
 */
data class SimpleJdbcConnectionProvider(override val name: String, val template: NamedParameterJdbcTemplate, override val jdbcDriver: JdbcDriver) :
   JdbcConnectionProvider {
   override fun build(): NamedParameterJdbcTemplate = template

   override val address: String = "Not provided"
   override val driver: String = "Not provided"
}

interface JdbcConnectionProvider : Connector {
   fun build(): NamedParameterJdbcTemplate
   override val type: ConnectorType
      get() = ConnectorType.JDBC
   val jdbcDriver:JdbcDriver
}

class JdbcConnectionRegistry(providers: List<JdbcConnectionProvider> = emptyList()) {
   private val connections: MutableMap<String, JdbcConnectionProvider> =
      providers.associateBy { it.name }.toMutableMap()

   fun hasConnection(name: String): Boolean = connections.containsKey(name)

   fun getConnection(name: String): JdbcConnectionProvider =
      connections[name] ?: error("No JdbcConnection with name $name is registered")

   fun register(provider: JdbcConnectionProvider) {
      connections[provider.name] = provider
   }

   fun listAll(): List<JdbcConnectionProvider> {
      return this.connections.values.toList()
   }
}


