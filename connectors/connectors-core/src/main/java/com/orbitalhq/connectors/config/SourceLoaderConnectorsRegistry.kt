package com.orbitalhq.connectors.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.config.*
import com.orbitalhq.connectors.VyneConnectionsConfig
import com.orbitalhq.connectors.config.hazelcast.HazelcastConfiguration
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
   /**
    * To enable writing, pass a writerProvider - (normally a
    * ProjectManagerConfigSourceLoader) here
    */
   writerProviders: List<ConfigSourceWriterProvider> = emptyList(),
   fallback: Config = ConfigFactory.systemEnvironment(),
) : MergingHoconConfigRepository<ConnectionsConfig>(loaders, writerProviders, fallback) {

   companion object {
      fun forPath(
         path: Path, fallback: Config = ConfigFactory.systemEnvironment()
      ): SourceLoaderConnectorsRegistry {
         val loader = FileConfigSourceLoader(
            path,
            packageIdentifier = VyneConnectionsConfig.PACKAGE_IDENTIFIER,
            failIfNotFound = false
         )

         return SourceLoaderConnectorsRegistry(
            loaders = listOf(loader),
            writerProviders = listOf(SimpleConfigSourceWriterProvider(loader)),
            fallback
         )

      }
   }

   override fun extract(config: Config): ConnectionsConfig = config.extract()

   override fun emptyConfig(): ConnectionsConfig {
      return ConnectionsConfig()
   }

   fun load(): ConnectionsConfig = typedConfig()

   fun loadUnresolvedConfig(packageIdentifier: PackageIdentifier): Config {
      val writer = this.getWriter(packageIdentifier)
      return loadUnresolvedConfig(writer, packageIdentifier)
   }

   fun saveConfig(packageIdentifier: PackageIdentifier, config: Config) {
      val writer = this.getWriter(packageIdentifier)
      writer.saveConfig(config)
      invalidateCache()
   }

   fun defaultHazelcastConfiguration(): HazelcastConfiguration? {
      val hazelcastConnectors = load().hazelcast
      return hazelcastConnectors.values.firstOrNull { it.default }
   }

   fun hazelcastConfigurationForConnectionName(connectionName: String): HazelcastConfiguration? {
      return load().hazelcast[connectionName]
   }
}


