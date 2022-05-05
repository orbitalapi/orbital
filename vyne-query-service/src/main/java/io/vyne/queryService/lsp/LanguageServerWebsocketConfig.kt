package io.vyne.queryService.lsp

import io.vyne.schema.consumer.SchemaStore
import io.vyne.schemas.SchemaSetChangedEvent
import mu.KotlinLogging
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.language-server")
data class LanguageServerConfig(
   val maxClients: Int = 100,
   val path: String = "/api/language-server",
)

private val logger = KotlinLogging.logger {  }

/**
 * Very simply listener that responds to schema changed
 * events, to trigger the LanguageServer to reload it's sources.
 * Most of the LSP infra is constructed on-demand outside of spring,
 * so this class acts as a bridge
 */
@Component
class SchemaChangedLspListener(private val schemaStore: SchemaStore): InitializingBean {
   private val handlers = mutableListOf<(SchemaSetChangedEvent) -> Unit>()
   fun registerHandler(handler: (SchemaSetChangedEvent) -> Unit) {
      this.handlers.add(handler)
   }

   override fun afterPropertiesSet() {
      Flux.from(schemaStore.schemaChanged).subscribe { schemaChangedEvent ->
         logger.info { "Schema set changes, notifying Language server client bridge" }
         handlers.forEach { handler ->
          try {
             handler(schemaChangedEvent)
          } catch (e: Exception) {
             logger.error (e) { "Error invoking Schema Set Changed Event handler $handler"  }
          }
         }
      }
   }
}
