package com.orbitalhq.connectors.config

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.ResultWithMessage
import com.orbitalhq.config.ConfigSourceLoader
import com.orbitalhq.config.ConfigSourceWriterProvider
import com.orbitalhq.config.FileConfigSourceLoader
import com.orbitalhq.config.MergingHoconConfigRepository
import com.orbitalhq.config.SimpleConfigSourceWriterProvider
import com.orbitalhq.connectors.VyneConnectionsConfig
import com.orbitalhq.connectors.config.hazelcast.HazelcastConfiguration
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
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

   fun saveConfig(packageIdentifier: PackageIdentifier, config: Config):ResultWithMessage {
      val writer = this.getWriter(packageIdentifier)
      val result = writer.saveConfig(config)
      invalidateCache()
      return result
   }

   fun defaultHazelcastConfiguration(): HazelcastConfiguration? {
      val hazelcastConnectors = load().hazelcast
      val defaultConnections = hazelcastConnectors.values.filter { it.default }
      if (defaultConnections.size > 1) {
         throw IllegalArgumentException("Only One Hazelcast connection can be defined as default - ${defaultConnections.joinToString { defaultConn -> defaultConn.connectionName }} marked as default!")
      }
      return defaultConnections.firstOrNull()
   }

   fun hazelcastConfigurationForConnectionName(connectionName: String): HazelcastConfiguration? {
      return load().hazelcast[connectionName]
   }
}


