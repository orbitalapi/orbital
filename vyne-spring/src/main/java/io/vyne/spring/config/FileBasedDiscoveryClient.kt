package io.vyne.spring.config

import io.vyne.http.ServicesConfig
import io.vyne.http.ServicesConfigRepository
import mu.KotlinLogging
import org.springframework.cloud.client.DefaultServiceInstance
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.DiscoveryClient
import java.net.URI
import java.nio.file.Path


abstract class BaseConfigDiscoveryClient : DiscoveryClient {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   abstract fun servicesConfig(): ServicesConfig
   override fun getInstances(serviceId: String): MutableList<ServiceInstance> {
      val services = try {
         servicesConfig().services
      } catch (e: Exception) {
         logger.error(e) { "An exception was thrown while loading / parsing the config from the repository: ${e.message}" }
         emptyMap()
      }
      val serviceAddress = services[serviceId] ?: emptyMap()
      return if (serviceAddress.isEmpty()) {
         mutableListOf()
      } else {
         mutableListOf(FileBasedDiscoveryClient.serviceInstance(serviceId, serviceAddress))
      }
   }

   override fun getServices(): MutableList<String> {
      return servicesConfig().services.keys.toMutableList()
   }
}

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
class FileBasedDiscoveryClient(private val configRepository: ServicesConfigRepository) : BaseConfigDiscoveryClient() {
   constructor(path: Path) : this(ServicesConfigRepository(path))

   companion object {
      const val urlParameter = "url"
      private val logger = KotlinLogging.logger {}

      private val LOCALHOST: DefaultServiceInstance = DefaultServiceInstance(
         "localhost",
         "localhost",
         "localhost",
         0,
         false
      )

      fun serviceInstance(serviceId: String, serviceConfiguration: Map<String, String>): DefaultServiceInstance {
         val url = serviceConfiguration[urlParameter]
         if (url == null) {
            val message = "Service $serviceId does not porivde a $urlParameter parameter"
            logger.error { message }
            error(message)
         }
         val uri = try {
            URI.create(url)
         } catch (e: Exception) {
            logger.error(e) { "Failed to parse $url as a valid URL." }
            throw e
         }

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

   override fun servicesConfig(): ServicesConfig {
      return configRepository.load()
   }


   override fun description(): String = "File based discovery client using config at ${configRepository.path}"
}

class StaticServicesConfigDiscoveryClient(private val servicesConfig: ServicesConfig) : BaseConfigDiscoveryClient() {
   override fun servicesConfig(): ServicesConfig = servicesConfig

   override fun description(): String = "Static services config discovery client"
}


