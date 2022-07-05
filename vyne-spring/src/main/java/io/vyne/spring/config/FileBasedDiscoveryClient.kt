package io.vyne.spring.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.vyne.config.BaseHoconConfigFileRepository
import mu.KotlinLogging
import org.springframework.cloud.client.DefaultServiceInstance
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.DiscoveryClient
import java.net.URI
import java.nio.file.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.absolutePathString

/**
 * A discovery client implementation which watches a HOCON
 * config file, and exposes discovery services based on its contents.
 *
 * This is useful for two scenarios:
 *  - a Docker / Docker Compose / Docker Swarm / k8s deployment where discovery is handled at the network layer
 *  - Local development, where you don't want to run Eureka, and you want to provide access to locally running services against a dockerized Vyne instance
 *
 * If the file doesn't exist on startup, a default file is created.
 */
class FileBasedDiscoveryClient(private val path: Path) : DiscoveryClient {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   private val configRepository: ServicesConfigRepository

   init {
      if (!Files.exists(path)) {
         writeDefaultConfigFile(path)
      }
      configRepository = ServicesConfigRepository(path)
   }

   fun watchForChanges() {
      configRepository.watchForChanges()
   }


   private fun writeDefaultConfigFile(path: Path) {
      logger.info { "Using a file based service mapping, but no config file found at $path so writing a default file" }
      val repository = ServicesConfigRepository(path)
      repository.writeDefault()

   }

   override fun description(): String = "File based discovery client using config at $path"

   override fun getInstances(serviceId: String): MutableList<ServiceInstance> {
      val services = configRepository.load().services
      val serviceAddress = services[serviceId] ?: return mutableListOf()
      return mutableListOf(serviceInstance(serviceId, serviceAddress))
   }

   private fun serviceInstance(serviceId: String, url: String): DefaultServiceInstance {
      val uri = URI.create(url)
      return DefaultServiceInstance(
         serviceId, serviceId, uri.host, uri.port, uri.scheme == "https"
      )
   }


   override fun getServices(): MutableList<String> {
      return configRepository.load().services.keys.toMutableList()
   }
}

class ServicesConfigRepository(
   private val configFilePath: Path,
   fallback: Config = ConfigFactory.systemProperties()
) : BaseHoconConfigFileRepository<ServicesConfig>(configFilePath, fallback) {
   private val logger = KotlinLogging.logger {}
   private val watchService = FileSystems.getDefault().newWatchService()
   private var watcherThread: Thread? = null
   private val registeredWatchKeys = mutableListOf<WatchKey>()
   override fun extract(config: Config): ServicesConfig = config.extract()

   override fun emptyConfig(): ServicesConfig = ServicesConfig.DEFAULT

   fun load(): ServicesConfig = typedConfig()

   companion object {
      private val registeredKeys = ArrayList<WatchKey>()
      private val watchServiceRef = AtomicReference<WatchService>()
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
   val services: Map<String, String>
) {
   companion object {
      val DEFAULT = ServicesConfig(
         mapOf(
            "schema-server" to "http://schema-server",
            "query-server" to "http://vyne",
            "pipeline-runner" to "http://vyne-pipeline-runner",
            "cask-server" to "http://cask",
            "analytics-server" to "http://vyne-analytics-server"
         )
      )
   }
}
