package io.vyne.spring.config

import mu.KotlinLogging
import org.springframework.cloud.client.DefaultServiceInstance
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.DiscoveryClient
import java.net.URI
import java.nio.file.*

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
class FileBasedDiscoveryClient(private val configRepository: ServicesConfigRepository) : DiscoveryClient {
   constructor(path: Path) : this(ServicesConfigRepository(path))

   companion object {
      const val urlParameter = "url"
      private val logger = KotlinLogging.logger {}

      fun serviceInstance(serviceId: String, serviceConfiguration: Map<String, String>): DefaultServiceInstance {
         val url = serviceConfiguration[urlParameter]
         val uri = URI.create(url)
         return DefaultServiceInstance(
            serviceId,
            serviceId,
            uri.host,
            uri.port,
            uri.scheme == "https",
            serviceConfiguration.map { it.key to it.value }.toMap()
         )
      }
   }

   val path: Path
      get() = configRepository.path


   fun watchForChanges() {
      configRepository.watchForChanges()
   }


   override fun description(): String = "File based discovery client using config at ${configRepository.path}"

   override fun getInstances(serviceId: String): MutableList<ServiceInstance> {
      val services = try {
         configRepository.load().services
      } catch (e: Exception) {
         logger.error(e) { "An exception was thrown while loading / parsing the config from the repository: ${e.message}" }
         emptyMap()
      }
      val serviceAddress = services[serviceId] ?: emptyMap()
      return mutableListOf(serviceInstance(serviceId, serviceAddress))
   }

   override fun getServices(): MutableList<String> {
      return configRepository.load().services.keys.toMutableList()
   }
}


