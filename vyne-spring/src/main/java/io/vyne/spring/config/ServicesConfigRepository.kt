package io.vyne.spring.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.vyne.config.BaseHoconConfigFileRepository
import mu.KotlinLogging
import reactor.core.publisher.Sinks
import java.nio.file.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.absolutePathString

class ServicesConfigRepository(
   private val configFilePath: Path,
   fallback: Config = ConfigFactory.systemEnvironment(),
   createConfigFileIfMissing: Boolean = true
) : BaseHoconConfigFileRepository<ServicesConfig>(configFilePath, fallback) {

   private var watcherThread: Thread? = null
   private val registeredWatchKeys = mutableListOf<WatchKey>()
   override fun extract(config: Config): ServicesConfig = config.extract()

   override fun emptyConfig(): ServicesConfig = ServicesConfig.DEFAULT


   init {
      if (!Files.exists(configFilePath) && createConfigFileIfMissing) {
         logger.info { "Using a file based service mapping, but no config file found at $path so writing a default file" }
         writeDefaultConfigFile(path)
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
         ServicesConfigRepository(path, createConfigFileIfMissing = false).writeDefault()
      }
   }


   fun writeDefault(): ServicesConfig {
      this.load()
      this.saveConfig(unresolvedConfig())
      return typedConfig()
   }

   fun stopWatching() {
      this.watcherThread?.interrupt()
   }

   fun watchForChanges() {
      synchronized(this) {

         this.watcherThread = Thread(Runnable {
            val canonicalParentPath = configFilePath.toFile().canonicalFile.parentFile.toPath()
            logger.info("Starting to watch $canonicalParentPath")
            val watchService = FileSystems.getDefault().newWatchService()
            registeredWatchKeys.add(
               canonicalParentPath.register(
                  watchService,
                  StandardWatchEventKinds.ENTRY_CREATE,
                  StandardWatchEventKinds.ENTRY_DELETE,
                  StandardWatchEventKinds.ENTRY_MODIFY,
                  StandardWatchEventKinds.OVERFLOW
               )
            )

            try {
               while (true) {
                  val key = watchService.take()
                  key.pollEvents()
                     .mapNotNull {
                        it.context() as? Path
                     }
                     .filter { changedPath ->
                        val changedPathString = canonicalParentPath.resolve(changedPath).absolutePathString()
                        changedPathString == configFilePath.absolutePathString()
                     }
                     .distinctBy { changedPath -> changedPath.absolutePathString() }
                     .forEach { changedPath ->
                        logger.info { "Detected file change in services config file at $changedPath. Invalidating cache and will reload service on next request" }
                        invalidateCache()
                     }
                  key.reset()
               }
            } catch (e: ClosedWatchServiceException) {
               logger.warn(e) { "Watch service was closed. ${e.message}" }
            } catch (e: Exception) {
               logger.error(e) { "Error in watch service: ${e.message}" }
            }
         })
         watcherThread!!.start()
      }
   }


}

data class ServicesConfig(
   val services: Map<String, Map<String, String>>
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
