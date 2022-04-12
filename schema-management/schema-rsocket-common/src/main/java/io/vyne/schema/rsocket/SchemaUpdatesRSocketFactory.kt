package io.vyne.schema.rsocket

import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.rsocket.RSocket
import io.rsocket.core.RSocketConnector
import io.rsocket.core.RSocketServer
import io.rsocket.metadata.WellKnownMimeType
import io.rsocket.transport.netty.client.TcpClientTransport
import io.rsocket.transport.netty.client.WebsocketClientTransport
import io.vyne.schema.api.AddressSupplier
import mu.KotlinLogging
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.net.URI
import java.net.URL
import java.time.Duration

/**
 * Builds an RSocket factory which publishes and consumes
 * updates about schema changes
 */
class SchemaUpdatesRSocketFactory(private val addressSupplier: AddressSupplier) {
   constructor(address: String) : this(AddressSupplier.just(URI.create(address)))

   private val logger = KotlinLogging.logger {}
   fun build(): Mono<RSocket> {
      val websocket = WebsocketClientTransport.create(addressSupplier.nextAddress())
      val tcpTransport = TcpClientTransport.create("localhost",7655)
      return RSocketConnector
         .create()
         .metadataMimeType(WellKnownMimeType.APPLICATION_JSON.string)
         .dataMimeType(WellKnownMimeType.APPLICATION_JSON.string)
         .reconnect(Retry.backoff(Long.MAX_VALUE, Duration.ofMillis(500)))
         .connect(tcpTransport)
   }
}

object CBORJackson {
   val defaultMapper = CBORMapper()
      .registerKotlinModule()
}
