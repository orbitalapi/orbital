package com.orbitalhq.connectors.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.config.*
import com.orbitalhq.connectors.VyneConnectionsConfig
import java.nio.file.Path

/**
 * A Connector registry, which describes connections to other data
 * sources (databases, kafka brokers, etc).
 *
 * This class is responsible for reading/writing a ConnectionsConfig
 * class (which is the actual config) from other sources - such as config
 * files, and schemas.
 *
 * This is the preferred approach for loading connections, as it
 * uses loaders, which supports pulling from schemas etc.
 */
class SourceLoaderConnectorsRegistry(
   private val loaders: List<ConfigSourceLoader>,
   fallback: Config = ConfigFactory.systemEnvironment(),
) : MergingHoconConfigRepository<ConnectionsConfig>(loaders, fallback) {

   private val writers = loaders.filterIsInstance<ConfigSourceWriter>()

   // for testing
   constructor(path: Path, fallback: Config = ConfigFactory.systemEnvironment()) : this(
      listOf(
         FileConfigSourceLoader(
            path,
            packageIdentifier = VyneConnectionsConfig.PACKAGE_IDENTIFIER,
            failIfNotFound = false
         )
      ),
      fallback
   )

   override fun extract(config: Config): ConnectionsConfig = config.extract()

   override fun emptyConfig(): ConnectionsConfig {
      return ConnectionsConfig()
   }

   fun load(): ConnectionsConfig = typedConfig()

   fun loadUnresolvedConfig(packageIdentifier: PackageIdentifier): Config {
      val writer = writers.getWriter(packageIdentifier,"connections.conf")
      return loadUnresolvedConfig(writer, packageIdentifier)
   }

   fun saveConfig(packageIdentifier: PackageIdentifier, config: Config) {
      val writer = writers.getWriter(packageIdentifier,"connections.conf")
      writer.saveConfig(packageIdentifier, config)
      invalidateCache()
   }

}


