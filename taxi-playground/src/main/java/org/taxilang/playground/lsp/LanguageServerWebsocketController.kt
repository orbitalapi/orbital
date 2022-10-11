package org.taxilang.playground.lsp

import com.google.common.cache.CacheBuilder
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.BaseUnits
import lang.taxi.lsp.sourceService.WorkspaceSourceServiceFactory
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketSession
import org.taxilang.playground.WebSocketController
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicInteger

/**
 * Simple websocket handler, which defers messages off to an instance
 * of the Taxi Language server, which is bound to the specific websocket session.
 */
@Component
class LanguageServerWebsocketController(
   maximumSize: Int = 100,
   private val sourceServiceFactory: WorkspaceSourceServiceFactory,
   private val config: LanguageServerConfig,
   metricsRegistry: MeterRegistry
) : WebSocketController {

   private val activeSessions = AtomicInteger(0)

   init {
      Gauge.builder("voyager.lsp.active-connections", { activeSessions })
         .baseUnit(BaseUnits.SESSIONS)
         .register(metricsRegistry)
   }

   private val logger = KotlinLogging.logger {}
   private val languageServerCache = CacheBuilder
      .newBuilder()
      .maximumSize(maximumSize.toLong())
      .build<WebSocketSession, WebsocketSessionLanguageServer>()

   override val paths: List<String> = listOf(config.path)
   override fun handle(session: WebSocketSession): Mono<Void> {
      val languageServer = WebsocketSessionLanguageServer(sourceServiceFactory)
      session.receive()
         .subscribe { message -> languageServer.consume(message.payloadAsText) }
      languageServerCache.put(session, languageServer)
      logger.info { "New LSP connection started with session ${session.id}" }
      activeSessions.incrementAndGet()
      logSizes()
      return session.send(languageServer.messages
         .doFinally {
            logger.info { "LSP connection on session ${session.id} closed" }
            activeSessions.decrementAndGet()
            languageServerCache.invalidate(session)
            logSizes()
         }
         .map { message -> session.textMessage(message) })

   }

   private fun logSizes() {
      logger.info { "Current counts: ${activeSessions.get()} sessions, cache size of ${languageServerCache.size()}" }
   }


}

