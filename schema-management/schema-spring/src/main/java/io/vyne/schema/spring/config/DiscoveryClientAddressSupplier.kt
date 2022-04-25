package io.vyne.schema.spring.config

import com.google.common.collect.Iterators
import io.vyne.schema.api.AddressSupplier
import io.vyne.schema.rsocket.TcpAddress
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.DiscoveryClient

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
   private val converter: (ServiceInstance) -> T
) : AddressSupplier<T> {

   private var instanceList: List<ServiceInstance> = emptyList()
   private var iterator: Iterator<ServiceInstance> = Iterators.cycle(instanceList)

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

   override fun nextAddress(): T {
      val instances = discoveryClient.getInstances(serviceId)
      if (instances.isEmpty()) {
         throw ServiceNotFoundException("Service $serviceId was not present in the discovery client")
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

   override val addresses: List<T>
      get() = TODO("Not yet implemented")
}

class ServiceNotFoundException(message: String) : java.lang.RuntimeException(message)
