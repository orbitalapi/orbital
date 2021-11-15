package io.vyne.historyServer.server

import io.rsocket.core.RSocketServer
import io.rsocket.transport.netty.server.CloseableChannel
import io.rsocket.transport.netty.server.TcpServerTransport
import mu.KotlinLogging
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class SocketServerStarter(
   @Value("\${vyne.analytics-server.port:7654}") private val rsocketPort: Int,
   private val rsocketMessageHandler: RSocketMessageHandler
   ): InitializingBean {
   private var server: CloseableChannel? = null
   override fun afterPropertiesSet() {
      logger.info { "Starting RSocket Server on port $rsocketPort ..." }
      startRSocketServer()
   }

   private fun startRSocketServer() {
      RSocketServer
         .create(rsocketMessageHandler.responder())
         .bind(TcpServerTransport.create( rsocketPort))
         .subscribe {
            logger.info { "RSocket Server Channel Opened at address: ${it.address()}" }
            server = it
         }
   }
}
