package io.vyne.connectors.jdbc.registry

import io.vyne.connectors.config.ConfigFileConnectorsRegistry
import io.vyne.connectors.config.jdbc.JdbcConnectionConfiguration

/**
 * A wrapper around ConfigFileConnectorsRegistry
 * (which reloads as things like schemas and file sources change),
 * that then implements the JdbcConnectionRegistry.
 *
 * Suspect this isn't quite right, but it's late.
 */
class ReloadingJdbcConnectionRegistry(private val configFileConnectorsRegistry: ConfigFileConnectorsRegistry) :
   JdbcConnectionRegistry {
   private fun getCurrent(): JdbcConnectionRegistry {
      return configFileConnectorsRegistry.jbcConnectionRegistry()
   }

   override fun hasConnection(name: String): Boolean = getCurrent().hasConnection(name)

   override fun getConnection(name: String): JdbcConnectionConfiguration = getCurrent().getConnection(name)

   override fun register(connectionConfiguration: JdbcConnectionConfiguration) =
      error("Mutations are not supported on this implementation")

   override fun remove(connectionConfiguration: JdbcConnectionConfiguration) =
      error("Mutations are not supported on this implementation")

   override fun listAll(): List<JdbcConnectionConfiguration> = getCurrent().listAll()
}
