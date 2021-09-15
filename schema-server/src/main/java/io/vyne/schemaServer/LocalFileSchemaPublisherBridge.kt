package io.vyne.schemaServer

import arrow.core.Either
import io.vyne.schemaStore.LocalFileBasedSchemaRepository
import io.vyne.schemaStore.SchemaPublisher
import lang.taxi.errors
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Watches a local file schema repository, and publishes changes out to the
 * provided schema publisher
 */
@Component
class LocalFileSchemaPublisherBridge(
   val fileBasedSchemaStore: LocalFileBasedSchemaRepository,
   val schemaPublisher: SchemaPublisher
) {

   init {
      fileBasedSchemaStore.sourcesChanged
         .subscribe { message ->
            val result = schemaPublisher.submitSchemas(message.sources)
            when (result) {
               is Either.Left -> logger.warn { "Update of sources resulted in ${result.a.errors.errors().size} compilation errors." }
               is Either.Right -> logger.info { "Sources updated successfully" }
            }

         }
   }

   fun rebuildSourceList() =
      fileBasedSchemaStore.rebuildSourceList()


}
