package io.vyne.schema.api

import com.google.common.collect.Iterables
import java.net.URI

/**
 * API for supplying an address, allowing a circular wrap for client-side load balancing
 * against things like Eureka.
 *
 * Ensure no dependencies on Spring or similar libraries in here.
 *
 */
interface AddressSupplier<T> {
   fun nextAddress(): T

   val addresses:List<T>

   companion object {
      fun <T> just(address: T): AddressSupplier<T> = SimpleAddressSupplier(address)
      fun <T> just(addresses: List<T>): AddressSupplier<T> = SimpleAddressSupplier(addresses)
   }
}

class SimpleAddressSupplier<T>(override val addresses: List<T>) : AddressSupplier<T> {
   constructor(address: T) : this(listOf(address))

   private val iterable = Iterables.cycle(addresses).iterator()
   override fun nextAddress(): T {
      return iterable.next()
   }
}


