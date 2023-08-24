package io.vyne.connectors.jdbc.registry

import io.vyne.connectors.config.SourceLoaderConnectorsRegistry
import io.vyne.connectors.config.jdbc.JdbcConnectionConfiguration
import io.vyne.connectors.registry.ConnectionRegistry

interface JdbcConnectionRegistry : ConnectionRegistry<JdbcConnectionConfiguration> {
}


fun SourceLoaderConnectorsRegistry.jbcConnectionRegistry(): JdbcConnectionRegistry {
   return InMemoryJdbcConnectionRegistry(
      this.load().jdbc.values.toList()
   )
}
