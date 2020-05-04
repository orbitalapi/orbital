package io.vyne.spring.invokers

import io.vyne.schemas.Operation
import io.vyne.schemas.Service
import org.springframework.cloud.client.discovery.DiscoveryClient

interface ServiceUrlResolver {
   fun canResolve(service: Service, operation: Operation): Boolean
   fun makeAbsolute(url: String, service: Service, operation: Operation): String
}

class ServiceDiscoveryClientUrlResolver(val discoveryClient: ServiceDiscoveryClient = NoOpServiceDiscoveryClient()) : ServiceUrlResolver {
   override fun canResolve(service: Service, operation: Operation): Boolean = service.hasMetadata("ServiceDiscoveryClient")

   override fun makeAbsolute(url: String, service: Service, operation: Operation): String {
      val serviceName = service.metadata("ServiceDiscoveryClient").params["serviceName"] as String
      return discoveryClient.resolve(serviceName) + url.trimStart('/')
   }
}

class AbsoluteUrlResolver() : ServiceUrlResolver {
   override fun canResolve(service: Service, operation: Operation): Boolean {
      return operation.hasMetadata("HttpOperation")
         && operation.metadata("HttpOperation").params.containsKey("url")
         && operation.metadata("HttpOperation").params["url"].let { url ->
         val urlString = url as String
         urlString.startsWith("http://") || urlString.startsWith("https://")
      }
   }

   override fun makeAbsolute(url: String, service: Service, operation: Operation): String {
      return url;
   }
}

interface ServiceDiscoveryClient {
   fun resolve(serviceName: String): String
}

class NoOpServiceDiscoveryClient : ServiceDiscoveryClient {
   override fun resolve(serviceName: String): String = "http://$serviceName/"
}

class SpringServiceDiscoveryClient(private val discoveryClient: DiscoveryClient) : ServiceDiscoveryClient {
   override fun resolve(serviceName: String): String {
      val instances = discoveryClient.getInstances(serviceName)
      if (instances.isEmpty()) {
         throw IllegalArgumentException("Service $serviceName is not known to the provided DiscoveryClient")
      }
      // TODO : Consider round-robin with Ribbon, or to randomize
      val instance = instances.first()
      return instance.uri.toASCIIString()
   }

}
