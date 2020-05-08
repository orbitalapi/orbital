package io.vyne.spring.invokers

import io.vyne.schemas.Operation
import io.vyne.schemas.Service
import io.vyne.spring.*
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.web.util.UriComponentsBuilder

interface ServiceUrlResolver {
   fun canResolve(service: Service, operation: Operation): Boolean
   fun makeAbsolute(url: String, service: Service, operation: Operation): String
}

class ServiceDiscoveryClientUrlResolver(val discoveryClient: ServiceDiscoveryClient = NoOpServiceDiscoveryClient()) : ServiceUrlResolver {
   override fun canResolve(service: Service, operation: Operation): Boolean = service.isServiceDiscoveryClient()

   override fun makeAbsolute(url: String, service: Service, operation: Operation): String {
      val serviceName = service.serviceDiscoveryClientName()
      return UriComponentsBuilder
         .fromUriString(discoveryClient.resolve(serviceName))
         .path(url)
         .build()
         .toUriString()
   }
}

class AbsoluteUrlResolver() : ServiceUrlResolver {
   override fun canResolve(service: Service, operation: Operation): Boolean {
      return operation.hasHttpMetadata() && operation.isHttpOperation()
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
