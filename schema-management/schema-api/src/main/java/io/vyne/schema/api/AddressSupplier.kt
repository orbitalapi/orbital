package io.vyne.schema.api

import com.google.common.collect.Iterables
import reactor.core.publisher.Mono
import java.net.URI

/**
 * API for supplying an address, allowing a circular wrap for client-side load balancing
 * against things like Eureka.
 *
 * Ensure no dependencies on Spring or similar libraries in here.
 *
 */
interface AddressSupplier<T> {
   fun nextAddress(): Mono<T>

   val addresses: Mono<List<T>>

   companion object {
      fun <T> just(address: T): AddressSupplier<T> = SimpleAddressSupplier(address)
      fun <T> just(addresses: List<T>): AddressSupplier<T> = SimpleAddressSupplier(addresses)
   }
}

class SimpleAddressSupplier<T>(addressList: List<T>) : AddressSupplier<T> {
   constructor(address: T) : this(listOf(address))

   override val addresses: Mono<List<T>> = Mono.just(addressList)
   private val iterable = Iterables.cycle(addressList).iterator()
   override fun nextAddress(): Mono<T> {
      return Mono.just(iterable.next())
   }
}


