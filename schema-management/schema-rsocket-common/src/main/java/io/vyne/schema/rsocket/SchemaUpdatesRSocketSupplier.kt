package io.vyne.schema.rsocket

import io.rsocket.RSocket
import io.rsocket.core.RSocketConnector
import io.rsocket.transport.netty.client.WebsocketClientTransport
import io.vyne.schema.api.AddressSupplier
import mu.KotlinLogging
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration

class SchemaUpdatesRSocketSupplier(private val addressSupplier: AddressSupplier) {
   private val logger = KotlinLogging.logger {}
   fun build(): Mono<RSocket> {
      val websocket = WebsocketClientTransport.create(addressSupplier.nextAddress())
      return RSocketConnector
         .create()
         .dataMimeType("application/cbor")
         .reconnect(Retry.backoff(Long.MAX_VALUE, Duration.ofMillis(500)))
         .connect(websocket)
   }
}
