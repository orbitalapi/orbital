package com.orbitalhq.http

import com.orbitalhq.config.ConfigSourceLoader
import com.orbitalhq.config.ConfigSourceWriter
import com.orbitalhq.config.FileConfigSourceLoader
import com.orbitalhq.config.MergingHoconConfigRepository
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.github.config4k.toConfig
import mu.KotlinLogging
import java.nio.file.Files

class SourceLoaderServicesConfigRepository(
   /**
    * Config loaded from source packages
    */
   packageLoaders: List<ConfigSourceLoader>,
   /**
    * Config loaded from the system.
    * Made an intentional choice to segregate these,
    * as we want the behvaiour of writing out a default
    * system config if not already present. (This makes
    * configuring the service easier).
    *
    * This should ALSO be present in the packageLoaders,
    * to ensure consistent loading.
    */
   systemConfigLoader: FileConfigSourceLoader,
   fallback: Config = ConfigFactory.systemEnvironment(),
) : ServicesConfigProvider, MergingHoconConfigRepository<ServicesConfig>(packageLoaders, fallback) {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   override fun extract(config: Config): ServicesConfig = config.extract()

   override fun emptyConfig(): ServicesConfig {
      return ServicesConfig.DEFAULT
   }

   override fun loadConfig(): ServicesConfig = typedConfig()

   init {
      if (!systemConfigLoader.configFileExists()) {
         logger.info { "No services.conf file found at ${systemConfigLoader.configFilePath.toFile().canonicalPath} so writing a default file" }
         systemConfigLoader.saveConfig(emptyConfig().toConfig("services"))
      } else {
         logger.info { "Using services.conf at ${systemConfigLoader.configFilePath.toFile().absolutePath} for orbital system services" }
      }
   }
}
