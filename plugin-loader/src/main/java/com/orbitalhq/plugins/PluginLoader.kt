package com.orbitalhq.plugins

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import mu.KotlinLogging
import java.net.URI
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.writeText

data class PluginConfig(val plugins: List<PluginDescriptor> = emptyList())
data class PluginDescriptor(val url: String)
data class LoadedPlugin(val url: String, val name: String, val errorMessage: String? = null) {
   val wasSuccessful = errorMessage == null
}

class PluginLoader(
   /**
    * Paths to the actual plugins.conf files.
    *
    * For each item, if it's a path, we'll read from disk.
    * If it's a URL, we'll download to a temp location, then read from the downloaded path
    */
   private val pluginsConfPaths: List<String>
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
         val paths = resolveAllPaths()

         // 1. Load and merge all HOCON configurations from the provided paths
         val mergedConfig = mergeConfigurations(paths)

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

   private fun resolveAllPaths(): List<Path> {
      return pluginsConfPaths.mapNotNull { uriOrPath ->
         val isUrl = URI.create(uriOrPath).let { uri ->
            uri.scheme == "http" || uri.scheme == "https"
         }
         if (isUrl) {
            downloadToTempFile(uriOrPath)
         } else {
            Paths.get(uriOrPath)
         }
      }
   }

   private fun downloadToTempFile(uriOrPath: String): Path? {
      val url = URI.create(uriOrPath).toURL()
      logger.info { "Downloading plugin.conf from $uriOrPath" }
      val text = try {
         url.readText()
      } catch (e: Exception) {
         logger.error(e) { "Failed to download plugin.conf from $uriOrPath" }
         return null
      }


      val tempFile = try {
         val tempFile = kotlin.io.path.createTempFile(prefix = "plugins", suffix = ".conf")
         tempFile.writeText(text)
         logger.info { "Downloaded plugin.conf from $uriOrPath to ${tempFile.toAbsolutePath()}" }
         tempFile
      } catch (e: Exception) {
         logger.error(e) { "Failed to write downloaded plugin.conf from $uriOrPath" }
         return null
      }
      return tempFile
   }

   private fun mergeConfigurations(paths: List<Path>): com.typesafe.config.Config {
      val absolutePaths = paths.map { it.toAbsolutePath() }
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
      val loadedServices = serviceLoader.toList()
      if (loadedServices.isEmpty()) {
         logger.warn { "Plugin at $jarUrl was loaded, but did not contain any plugins" }
      }
      loadedServices.forEach { plugin ->
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
