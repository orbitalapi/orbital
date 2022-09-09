package io.vyne.spring.config

import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient
import reactor.core.publisher.Flux

/**
 * A reactive wrapper around the FileBasedDiscoveryClient.
 *
 * Implementation notes:
 * It looks like Spring calls getInstances() and getServices() frequently, but not on every request.
 * That's confusing.
 *
 * I somewhat expected it would call it once per serviceId, and then listen on the flux for changes.
 * I tried an implementation where we used a Flux<> driven from the underlying change listener in the
 * BaseHoconConfigFileRepository, but that lead to flux<> confusion, where often
 * the current set of values wasn't replayed consistently.
 *
 * Given the caller seems to expect a short-lived flux, we're just returning that.
 *
 * However, there is a chance that if the address of a server changes inbetween calls to getInstances()
 * then even if the file system picks it up, the change won't be broadcast on the Flux<>.
 */
class FileBasedReactiveDiscoveryClient(private val discoveryClient: FileBasedDiscoveryClient) :
   ReactiveDiscoveryClient {
   override fun description(): String = "Reactive file based discovery client, using config at ${discoveryClient.path}"


   override fun getInstances(serviceId: String): Flux<ServiceInstance> {
      return Flux.fromIterable(discoveryClient.getInstances(serviceId))
   }

   override fun getServices(): Flux<String> {
      return Flux.fromIterable(discoveryClient.services)
   }

}
