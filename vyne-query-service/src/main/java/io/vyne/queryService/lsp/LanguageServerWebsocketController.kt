package io.vyne.queryService.lsp

import com.google.common.cache.CacheBuilder
import io.vyne.queryService.WebSocketController
import io.vyne.schema.api.SchemaProvider
import lang.taxi.lsp.sourceService.WorkspaceSourceServiceFactory
import lang.taxi.packages.utils.log
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

