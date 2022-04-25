package io.vyne.schema.rsocket

import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.rsocket.RSocket
import io.vyne.schema.api.AddressSupplier
import io.vyne.schema.api.SimpleAddressSupplier
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

/**
 * Builds an RSocket factory which publishes and consumes
 * updates about schema changes
 */
class SchemaServerRSocketFactory(
   private val addresses: AddressSupplier<ClientTransportAddress>
) {
   constructor(
      address: ClientTransportAddress,
   ) : this(SimpleAddressSupplier(listOf(address)))

   constructor(
      addresses: List<ClientTransportAddress>,
   ) : this(SimpleAddressSupplier(addresses))

   private val logger = KotlinLogging.logger {}
   private val rsocketSink = Sinks.many().replay().latest<RSocket>()

   /**
    * Emits a flux of RSocket connections.
    * Each time a new RSocket connection is established, a new
    * RSocket is emitted.  See ReconnectInitiallyRSocket for details on why.
    */
   val rsockets: Flux<RSocket>
      get() {
         return rsocketSink.asFlux()
      }

   init {
      emitNewRSocket()
   }

   private fun emitNewRSocket() {
      val address = addresses.nextAddress()
      logger.info { "Attempting to connect to Schema Server on address $address" }
      val rsocketWrapper = ReconnectInitiallyRSocket(address)
      rsocketWrapper.terminal.doOnTerminate {
         logger.info { "RSocket has disconnected.  Will build a new one" }
         emitNewRSocket()
      }.subscribe()

      rsocketWrapper.rsocket.subscribe { rsocket ->
         rsocketSink.emitNext(rsocket) { signalType, emitResult ->
            logger.error("Failed to emit rsocket: $signalType $emitResult")
            true
         }
      }

   }

}

object CBORJackson {
   val defaultMapper = CBORMapper()
      .registerKotlinModule()
}




