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
interface AddressSupplier {
   fun nextAddress(): URI

   companion object {
      fun just(uri: URI): AddressSupplier = SimpleAddressSupplier(uri)
      fun just(uris: List<URI>): AddressSupplier = SimpleAddressSupplier(uris)
   }
}

class SimpleAddressSupplier(uris: List<URI>) : AddressSupplier {
   constructor(uri: URI) : this(listOf(uri))

   private val iterable = Iterables.cycle(*uris.toTypedArray()).iterator()
   override fun nextAddress(): URI {
      return iterable.next()
   }
}
