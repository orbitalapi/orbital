package io.vyne.queryService.lsp

import io.vyne.schemaStore.SchemaSourceProvider
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.utils.log
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@EnableWebSocket
@Configuration
class LanguageServerWebsocketConfig : WebSocketConfigurer {
   @Autowired
   lateinit var schemaSourceProvider: SchemaSourceProvider

   @Autowired
   lateinit var languageServerConfig: LanguageServerConfig

   @Autowired
   lateinit var workspaceSourceServiceFactory: SchemaWorkspaceSourceServiceFactory

   @Autowired
   lateinit var schemaChangedListener: SchemaChangedLspListener

   override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
      log().info("Initializing language-server websocket client with config: $languageServerConfig")
      registry.addHandler(
         LspWebsocketHandler(
            languageServerConfig.maxClients,
            workspaceSourceServiceFactory,
            schemaChangedListener
         ), languageServerConfig.path
      ).setAllowedOrigins(languageServerConfig.allowedOrigins)
   }

}

@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.language-server")
data class LanguageServerConfig(
   val maxClients: Int = 100,
   val path: String = "/language-server",
   val allowedOrigins: String = "*"
)

/**
 * Very simply listener that responds to schema changed
 * events, to trigger the LanguageServer to reload it's sources.
 * Most of the LSP infra is constructed on-demand outside of spring,
 * so this class acts as a bridge
 */
@Component
class SchemaChangedLspListener {
   private val handlers = mutableListOf<(SchemaSetChangedEvent) -> Unit>()
   fun registerHandler(handler: (SchemaSetChangedEvent) -> Unit) {
      this.handlers.add(handler)
   }

   @EventListener
   fun onSchemaChanged(event: SchemaSetChangedEvent) {
      log().info("Schema set changes, notifying Language server client bridge")
      handlers.forEach { handler -> handler(event) }
   }
}
