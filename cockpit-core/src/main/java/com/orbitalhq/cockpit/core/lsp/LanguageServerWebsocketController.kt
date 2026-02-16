package com.orbitalhq.cockpit.core.lsp

import com.google.common.cache.CacheBuilder
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.spring.http.websocket.WebSocketController
import lang.taxi.lsp.sourceService.WorkspaceSourceServiceFactory
import lang.taxi.utils.log
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

/**
 * Simple websocket handler, which defers messages off to an instance
 * of the Taxi Language server, which is bound to the specific websocket session.
 */
@Component
class LanguageServerWebsocketController(
   maximumSize: Int = 100,
   private val sourceServiceFactory: WorkspaceSourceServiceFactory,
   private val schemaChangedListener: SchemaChangedLspListener,
   private val config: LanguageServerConfig,
   private val schemaProvider: SchemaProvider
) : WebSocketController {
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

   override val paths: List<String> = listOf(config.path)
   override fun handle(session: WebSocketSession): Mono<Void> {
      val languageServer = WebsocketSessionLanguageServer(sourceServiceFactory, schemaProvider.schema)
      session.receive()
         .onErrorResume { error ->
            log().info("Language server disconnected: ${error.message}")
            cleanupSession(session, languageServer)
            Mono.empty()
         }
         .subscribe(
            { message -> languageServer.consume(message.payloadAsText) },
            { error ->
               log().info("Language server error: ${error.message}")
               cleanupSession(session, languageServer)
            }
         )

      languageServerCache.put(session, languageServer)
      return session.send(
         languageServer.messages
            .map { message -> session.textMessage(message) })
         .doFinally {
            // Cleanup when the WebSocket session ends (normal close, error, or cancel)
            cleanupSession(session, languageServer)
         }

   }

   private fun cleanupSession(session: WebSocketSession, languageServer: WebsocketSessionLanguageServer) {
      log().info("Cleaning up language server for session ${session.id}")
      languageServerCache.invalidate(session)
      languageServer.shutdown()
   }

   private val languageServerCache = CacheBuilder
      .newBuilder()
      .maximumSize(maximumSize.toLong())
      .removalListener<WebSocketSession, WebsocketSessionLanguageServer> { notification ->
         // Shutdown language server when evicted from cache
         notification.value?.let { languageServer ->
            log().info("Language server evicted from cache (${notification.cause}), shutting down")
            languageServer.shutdown()
         }
      }
      .build<WebSocketSession, WebsocketSessionLanguageServer>()

}

