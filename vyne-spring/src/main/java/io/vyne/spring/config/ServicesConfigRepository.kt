package io.vyne.spring.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.vyne.config.ChangeWatchingConfigFileRepository
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories

class ServicesConfigRepository(
   private val configFilePath: Path,
   fallback: Config = ConfigFactory.systemEnvironment(),
   createConfigFileIfMissing: Boolean = true
) : ChangeWatchingConfigFileRepository<ServicesConfig>(configFilePath, fallback) {

   override fun extract(config: Config): ServicesConfig = config.extract()

   override fun emptyConfig(): ServicesConfig = ServicesConfig.DEFAULT


   init {
      if (!Files.exists(configFilePath) && createConfigFileIfMissing) {
         logger.info { "Using a file based service mapping, but no config file found at $path so writing a default file" }
         writeDefaultConfigFile(path)
      } else {
         logger.info { "Service mapping initiated at ${configFilePath}" }
      }
   }


   val path: Path
      get() {
         return configFilePath
      }

   fun load(): ServicesConfig = typedConfig()


   companion object {
      private val logger = KotlinLogging.logger {}

      fun writeDefaultConfigFile(path: Path) {
         path.parent.createDirectories()
         ServicesConfigRepository(path, createConfigFileIfMissing = false).writeDefault()
      }
   }


   fun writeDefault(): ServicesConfig {
      this.load()
      this.saveConfig(unresolvedConfig())
      return typedConfig()
   }
}

data class ServicesConfig(
   val services: Map<String, Map<String, String>> = emptyMap()
) {
   companion object {
      val DEFAULT = ServicesConfig(
         mapOf(
            "schema-server" to mapOf("url" to "http://schema-server", "rsocket-port" to "7655"),
            "query-server" to mapOf("url" to "http://vyne"),
            "pipeline-runner" to mapOf("url" to "http://vyne-pipeline-runner"),
            "cask-server" to mapOf("url" to "http://cask"),
            "analytics-server" to mapOf("url" to "http://vyne-analytics-server")
         )
      )
   }
}
