package io.vyne.schema.rsocket

import io.rsocket.RSocket
import io.rsocket.core.RSocketConnector
import io.rsocket.metadata.WellKnownMimeType
import mu.KotlinLogging
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.util.retry.Retry
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

/**
 * An RSocket connection factory that produces
 * an RSocket which only attempts to reconnect until an intiial
 * connection is established.
 *
 * Once a connection has been established, if the connection is dropped,
 * no further attempts to reconnect are made.
 *
 * Instead, a completion event is emitted on the "terminal" property,
 * which a caller can leverage to set up a new connection.
 *
 * This is mainly useful for consumers who need to do something when a connection
 * is first established (such as publish the schema).
 *
 * We've built this because the RSocket reconnection doesn't provide a mechansim
 * for detecting when a reconnection occurs
 *
 */
class ReconnectInitiallyRSocket(address: ClientTransportAddress) {
   object PoisonPill

   private val logger = KotlinLogging.logger {}

   /**
    * A sink that emits a PoisonPill once the RSocket connection has terminated,
    * and won't attempt to reconnect
    */
   private val terminalSink = Sinks.one<PoisonPill>()
   val terminal: Mono<PoisonPill> = terminalSink.asMono()

   private val isTerminated: AtomicBoolean = AtomicBoolean(false);
   val rsocket: Mono<RSocket> = RSocketConnector.create()
      .metadataMimeType(WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.string)
      .dataMimeType(WellKnownMimeType.APPLICATION_CBOR.string)
      .reconnect(
         Retry.fixedDelay(Long.MAX_VALUE, Duration.ofMillis(500))
            .filter { f ->
               !isTerminated.get()
            }
      )
      .connect(address.buildTransport())
      .doOnNext { rsocket ->
         rsocket.onClose()
            .doOnError { error ->
               logger.debug { "RSocket closed with error: ${error.message}.  Will trigger reconnection" }
               isTerminated.set(true)
               terminalSink.tryEmitEmpty()
            }
            .doFinally { signalType ->
               logger.debug { "RSocket closed: $signalType.  Will trigger reconnection" }
               isTerminated.set(true)
               terminalSink.tryEmitEmpty()
            }
            .subscribe()
      }
}
