package io.vyne.connectors.jdbc.registry

import io.vyne.connectors.jdbc.JdbcConnectionConfiguration
import io.vyne.connectors.jdbc.JdbcDriver
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

/**
 * Really just used in tests, where we already have a JdbcTemplate.
 */
data class SimpleJdbcTemplateProvider(val name: String, val template: NamedParameterJdbcTemplate, val jdbcDriver: JdbcDriver) :
   JdbcTemplateProvider {
   override fun build(): NamedParameterJdbcTemplate = template

//   override val address: String = "Not provided"
//   override val driver: String = "Not provided"
}

interface JdbcTemplateProvider {
   fun build(): NamedParameterJdbcTemplate
//   val jdbcDriver:JdbcDriver
}

class InMemoryJdbcConnectionRegistry(configs: List<JdbcConnectionConfiguration> = emptyList()) : JdbcConnectionRegistry {
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


