package io.vyne.schema.spring.config

import com.google.common.collect.Iterators
import io.vyne.schema.api.AddressSupplier
import io.vyne.schema.rsocket.TcpAddress
import mu.KotlinLogging
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.DiscoveryClient
import reactor.core.publisher.Mono
import reactor.kotlin.extra.retry.retryExponentialBackoff
import reactor.util.retry.Retry
import java.time.Duration

/**
 * Simple wrapper around the spring DiscoveryClient interface,
 * which provides AddressSupplier capabilities.
 *
 * Addresses are round-robined.
 *
 */
class DiscoveryClientAddressSupplier<T>(
   private val discoveryClient: DiscoveryClient,
   private val serviceId: String,
   private val converter: (ServiceInstance) -> T,
   private val retry: Retry = Retry.fixedDelay(Long.MAX_VALUE, Duration.ofMillis(1500))
) : AddressSupplier<T> {

   private var instanceList: List<ServiceInstance> = emptyList()
   private var iterator: Iterator<ServiceInstance> = Iterators.cycle(instanceList)
   private val logger = KotlinLogging.logger {}

   init {
      resetServices(emptyList())
   }

   companion object {
      fun TcpAddressConverter(fixedPort: Int?) = { serviceInstance: ServiceInstance ->
         TcpAddress(serviceInstance.host, fixedPort ?: serviceInstance.port)
      }

      fun forTcpAddresses(
         discoveryClient: DiscoveryClient,
         serviceId: String,
         fixedPort: Int?
      ): DiscoveryClientAddressSupplier<TcpAddress> {
         return DiscoveryClientAddressSupplier(discoveryClient, serviceId, TcpAddressConverter(fixedPort))
      }
   }

   override fun nextAddress(): Mono<T> {
      return Mono.defer {
         Mono.just(getNextAddressFromDiscoveryClient())
      }
         .retryWhen(retry)
//      val instances = discoveryClient.getInstances(serviceId)
//      if (instances.isEmpty()) {
//         throw ServiceNotFoundException("Service $serviceId was not present in the discovery client")
//      }
//      // If the set of instances changes, reset the iterator.
//      // Otherwise, just keep looping.
//      // This provides cheap round-robin implementation.
//      if (instances != instanceList) {
//         resetServices(instances)
//      }
//      return Mono.just(converter(iterator.next()))
   }

   private fun getNextAddressFromDiscoveryClient(): T {
      logger.debug { "Requesting instances of service $serviceId from discovery client ${discoveryClient::class.simpleName}" }
      val instances = discoveryClient.getInstances(serviceId)
      if (instances.isEmpty()) {
         val message = "Service $serviceId was not present in the discovery client"
         logger.info { message }
         // Throwing here allows us to use retry semantics in the Mono<>
         throw ServiceNotFoundException(message)
      }
      // If the set of instances changes, reset the iterator.
      // Otherwise, just keep looping.
      // This provides cheap round-robin implementation.
      if (instances != instanceList) {
         resetServices(instances)
      }
      return converter(iterator.next())
   }

   private fun resetServices(instances: List<ServiceInstance>) {
      instanceList = instances
      iterator = Iterators.cycle(instances)
   }

   override val addresses: Mono<List<T>>
      get() = error("Not supported on this type of supplier")
}

class ServiceNotFoundException(message: String) : java.lang.RuntimeException(message)
