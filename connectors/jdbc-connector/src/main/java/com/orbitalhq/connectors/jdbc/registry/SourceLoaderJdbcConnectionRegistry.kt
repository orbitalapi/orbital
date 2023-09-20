package com.orbitalhq.connectors.jdbc.registry

import com.orbitalhq.connectors.config.ConnectionsConfig
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.connectors.config.jdbc.JdbcConnectionConfiguration
import com.orbitalhq.connectors.registry.MutableConnectionRegistry
import com.orbitalhq.connectors.registry.SourceLoaderConnectionRegistryAdapter
import java.nio.file.Path

/**
 * A wrapper around ConfigFileConnectorsRegistry
 * (which reloads as things like schemas and file sources change),
 * that then implements the JdbcConnectionRegistry.
 *
 */
class SourceLoaderJdbcConnectionRegistry(
   sourceLoaderConnectorsRegistry: SourceLoaderConnectorsRegistry
) :
   JdbcConnectionRegistry,
   MutableConnectionRegistry<JdbcConnectionConfiguration>,
   SourceLoaderConnectionRegistryAdapter<JdbcConnectionConfiguration>(
      sourceLoaderConnectorsRegistry,
      ConnectionsConfig::jdbc
   ) {
   constructor(path: Path) : this(SourceLoaderConnectorsRegistry(path))
}
