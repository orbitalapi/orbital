package io.vyne.connectors.jdbc.registry

import io.vyne.connectors.config.SourceLoaderConnectorsRegistry
import io.vyne.connectors.config.jdbc.JdbcConnectionConfiguration
import io.vyne.connectors.registry.SourceLoaderConnectionRegistryAdapter

/**
 * A wrapper around ConfigFileConnectorsRegistry
 * (which reloads as things like schemas and file sources change),
 * that then implements the JdbcConnectionRegistry.
 *
 */
class SourceLoaderJdbcConnectionRegistry(sourceLoaderConnectorsRegistry: SourceLoaderConnectorsRegistry) :
   JdbcConnectionRegistry, SourceLoaderConnectionRegistryAdapter<JdbcConnectionConfiguration>(
   sourceLoaderConnectorsRegistry,
   { config -> config.jdbc }) {

}
