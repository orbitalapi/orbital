package com.orbitalhq.plugins

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import mu.KotlinLogging
import java.net.URI
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

data class PluginConfig(val plugins: List<PluginDescriptor> = emptyList())
data class PluginDescriptor(val url: String)
data class LoadedPlugin(val url: String, val name: String, val errorMessage: String? = null) {
   val wasSuccessful = errorMessage == null
}

class PluginLoader(
   /**
    * Paths to the actual plugins.conf files
    */
   private val pluginsConfPaths: List<Path>
) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   private val _loadedPlugins = mutableListOf<LoadedPlugin>()

   val loadedPlugins: List<LoadedPlugin>
      get() {
         return _loadedPlugins.toList()
      }

   fun loadPlugins(): List<LoadedPlugin> {
      try {
         // 1. Load and merge all HOCON configurations from the provided paths
         val mergedConfig = mergeConfigurations()

         // 2. Deserialize the merged configuration using config4k
         val pluginConfig = mergedConfig.extract<PluginConfig>()

         // 3. Iterate over the list of plugins and load them
         pluginConfig.plugins.forEach { descriptor ->
            try {
               logger.info { "Starting download for plugin: ${descriptor.url}" }
               loadAndInitializePlugin(descriptor.url)
            } catch (e: Exception) {
               logger.error(e) { "Failed to load plugin: ${descriptor.url}" }
            }
         }
      } catch (e: Exception) {
         logger.error(e) { "Failed to load plugins due to an unexpected error." }
      }

      return _loadedPlugins
   }

   private fun mergeConfigurations(): com.typesafe.config.Config {
      val absolutePaths = pluginsConfPaths.map { it.toAbsolutePath() }
      logger.info { "The following paths are being scanned for plugin config files: ${absolutePaths.joinToString(", ")}" }
      val configList = absolutePaths
         .filter { Files.exists(it) }
         .map { path ->
            logger.info { "Found plugins.conf file at $path" }
            ConfigFactory.parseReader(Files.newBufferedReader(path))
         }.toMutableList()

      // Include the system (OS) configuration
      configList.add(ConfigFactory.systemProperties())

      // Merge all configurations together
      return configList.reduce { acc, config -> acc.withFallback(config) }.resolve()
   }

   private fun loadAndInitializePlugin(jarUrl: String) {
      val url = URI.create(jarUrl).toURL()
      val classLoader = URLClassLoader(arrayOf(url), Thread.currentThread().contextClassLoader)

      // Use the ServiceLoader to find implementations of the Plugin interface
      val serviceLoader = ServiceLoader.load(Plugin::class.java, classLoader)

      serviceLoader.forEach { plugin ->
         try {
            logger.info { "Initializing plugin: ${plugin.name}" }
            plugin.initialize()
            _loadedPlugins.add(LoadedPlugin(url = jarUrl, name = plugin.name))
            logger.info { "Successfully initialized plugin: ${plugin.name}" }
         } catch (e: Exception) {
            logger.error(e) { "Failed to initialize plugin: ${plugin.name}" }
            _loadedPlugins.add(LoadedPlugin(url = jarUrl, name = plugin.name, errorMessage = e.message))
         }
      }
   }
}
