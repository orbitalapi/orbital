package io.vyne.queryService.lsp

import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.utils.log
import mu.KotlinLogging
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.language-server")
data class LanguageServerConfig(
   val maxClients: Int = 100,
   val path: String = "/api/language-server",
)

/**
 * Very simply listener that responds to schema changed
 * events, to trigger the LanguageServer to reload it's sources.
 * Most of the LSP infra is constructed on-demand outside of spring,
 * so this class acts as a bridge
 */
@Component
class SchemaChangedLspListener {
   private val logger = KotlinLogging.logger {}
   private val handlers = mutableListOf<(SchemaSetChangedEvent) -> Unit>()
   fun registerHandler(handler: (SchemaSetChangedEvent) -> Unit) {
      this.handlers.add(handler)
   }

   @EventListener
   fun onSchemaChanged(event: SchemaSetChangedEvent) {
      log().info("Schema set changes, notifying Language server client bridge")
      handlers.forEach { handler ->
         try {
            handler(event)
         } catch (e: Exception) {
            logger.error(e) { "Handler ${handler::class.simpleName} threw an exception when processing a ${event::class.simpleName}" }
         }
      }
   }
}
