package io.vyne.queryService.lsp

import com.google.common.cache.CacheBuilder
import lang.taxi.lsp.sourceService.WorkspaceSourceServiceFactory
import lang.taxi.packages.utils.log
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

/**
 * Simple websocket handler, which defers messages off to an instance
 * of the Taxi Language server, which is bound to the specific websocket session.
 */
class LspWebsocketHandler(
   maximumSize: Int = 100,
   private val sourceServiceFactory: WorkspaceSourceServiceFactory,
   private val schemaChangedListener: SchemaChangedLspListener
) : TextWebSocketHandler() {

   init {
      schemaChangedListener.registerHandler { (_) ->
         log().info("Received schema set changed event - triggering LSP to reload sources")
         this.languageServerCache
            .asMap()
            .values
            .forEach { languageServerClient ->
               languageServerClient.languageServer.forceReloadOfSources("Schema set changed")
            }
      }
   }

   private val languageServerCache = CacheBuilder
      .newBuilder()
      .maximumSize(maximumSize.toLong())
      .build<WebSocketSession, WebsocketSessionLanguageServer>()

   override fun afterConnectionEstablished(session: WebSocketSession) {
      log().info("New websocket session established - ${session.id} - spawning a new language server instance")
      this.languageServerCache.put(session, WebsocketSessionLanguageServer(session, sourceServiceFactory))
   }

   override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
      val languageServer = languageServerCache.getIfPresent(session)
      if (languageServer == null) {
         log().warn("Received a message on a websocket session (${session.id}), but no LSP instance has been created for the session - $message")
      } else {
         languageServer.handleTextMessage(message)
      }
   }

   override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
      super.afterConnectionClosed(session, status)
      log().info("Websocket session ${session.id} closed with status $status - killing the language server instance")
      this.languageServerCache.getIfPresent(session)?.shutdown()
      this.languageServerCache.invalidate(session)
   }
}

