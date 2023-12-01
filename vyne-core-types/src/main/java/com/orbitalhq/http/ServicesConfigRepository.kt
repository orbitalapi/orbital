package com.orbitalhq.http

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import com.orbitalhq.config.ChangeWatchingConfigFileRepository
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories

interface ServicesConfigProvider {
   fun loadConfig(): ServicesConfig
}

class ServicesConfigRepository(
   private val configFilePath: Path,
   fallback: Config = ConfigFactory.systemEnvironment(),
   createConfigFileIfMissing: Boolean = true
) : ServicesConfigProvider, ChangeWatchingConfigFileRepository<ServicesConfig>(configFilePath, fallback) {

   override fun extract(config: Config): ServicesConfig = config.extract()

   override fun emptyConfig(): ServicesConfig = ServicesConfig.DEFAULT


   init {
      if (!Files.exists(configFilePath) && createConfigFileIfMissing) {
         logger.info { "Using a file based service mapping, but no config file found at $path so writing a default file" }
         writeDefaultConfigFile(path)
      } else {
         logger.info { "Service mapping initiated at ${configFilePath.toFile().absolutePath}" }
      }
   }


   val path: Path
      get() {
         return configFilePath
      }

   override fun loadConfig(): ServicesConfig = typedConfig()


   companion object {
      private val logger = KotlinLogging.logger {}

      fun writeDefaultConfigFile(path: Path) {
         path.parent.createDirectories()
         ServicesConfigRepository(path, createConfigFileIfMissing = false).writeDefault()
      }
   }


   fun writeDefault(): ServicesConfig {
      this.loadConfig()
      this.saveConfig(unresolvedConfig())
      return typedConfig()
   }
}

