package io.vyne.historyServer.server

import io.vyne.query.history.VyneHistoryRecord
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.annotation.ConnectMapping
import org.springframework.messaging.rsocket.retrieveFlux
import org.springframework.stereotype.Controller
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks

private val logger = KotlinLogging.logger {}

@Controller
class RSocketService(private val messageSink: Sinks.Many<VyneHistoryRecord>) {
   val mimeType = MediaType.APPLICATION_JSON

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   @ConnectMapping
   fun handle(requester: RSocketRequester): Mono<Void> {
      requester.rsocket().onClose().subscribe { logger.warn { "Query history client closed the connection" } }
      logger.info { "A Query history client connected" }
      requester
         .route("analyticsRecords")
         .metadata { metadataSpec -> metadataSpec.metadata("", mimeType) }
         .retrieveFlux<String>()
         .subscribe {
            logger.debug { "Received event: $it" }
//            messageSink.tryEmitNext(it)
         }
      return Mono.empty()
   }
}
