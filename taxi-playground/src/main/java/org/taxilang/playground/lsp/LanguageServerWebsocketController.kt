package org.taxilang.playground.lsp

import com.google.common.cache.CacheBuilder
import lang.taxi.lsp.sourceService.WorkspaceSourceServiceFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketSession
import org.taxilang.playground.WebSocketController
import reactor.core.publisher.Mono

/**
 * Simple websocket handler, which defers messages off to an instance
 * of the Taxi Language server, which is bound to the specific websocket session.
 */
@Component
class LanguageServerWebsocketController(
   maximumSize: Int = 100,
   private val sourceServiceFactory: WorkspaceSourceServiceFactory,
   private val config: LanguageServerConfig,
) : WebSocketController {

   override val paths: List<String> = listOf(config.path)
   override fun handle(session: WebSocketSession): Mono<Void> {
      val languageServer = WebsocketSessionLanguageServer(sourceServiceFactory)
      session.receive()
         .subscribe { message -> languageServer.consume(message.payloadAsText) }
      languageServerCache.put(session, languageServer)
      return session.send(languageServer.messages
         .map { message -> session.textMessage(message) })

   }

   private val languageServerCache = CacheBuilder
      .newBuilder()
      .maximumSize(maximumSize.toLong())
      .build<WebSocketSession, WebsocketSessionLanguageServer>()

}

