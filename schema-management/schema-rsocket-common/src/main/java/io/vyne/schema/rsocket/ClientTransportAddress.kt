package io.vyne.schema.rsocket

import io.rsocket.transport.ClientTransport
import io.rsocket.transport.netty.client.TcpClientTransport

/**
 * Allows for specifying an addreses for an Rsocket connection
 * such as Tcp / Websocket etc
 *
 */
interface ClientTransportAddress {
   fun buildTransport(): ClientTransport
}

data class TcpAddress(val host: String, val port: Int) : ClientTransportAddress {
   override fun buildTransport(): ClientTransport {
      return TcpClientTransport.create(host, port)
   }
}
