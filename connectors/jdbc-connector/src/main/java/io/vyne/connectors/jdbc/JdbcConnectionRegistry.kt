package io.vyne.connectors.jdbc

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

/**
 * Really just used in tests, where we already have a JdbcTemplate.
 */
data class SimpleJdbcConnectionProvider(override val name: String, val template: NamedParameterJdbcTemplate) :
   JdbcConnectionProvider {
   override fun build(): NamedParameterJdbcTemplate = template
}

interface JdbcConnectionProvider {
   val name: String
   fun build(): NamedParameterJdbcTemplate
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


