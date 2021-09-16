package io.vyne.schemaServer.file

import arrow.core.Either
import io.vyne.schemaStore.FileSystemSchemaRepository
import io.vyne.schemaStore.SchemaPublisher
import io.vyne.schemas.Schema
import lang.taxi.CompilationException
import lang.taxi.errors
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Watches local file schema repositories, and publishes changes out to the
 * provided schema publisher.
 *
 * This class replaces the CompilerService, deferring compilation and validation to
 * the SchemaPublisher
 */
@Component
class FileChangeSchemaPublisher(
   val fileBasedSchemaStores: List<FileSystemSchemaRepository>,
   val schemaPublisher: SchemaPublisher
) {

   init {
      fileBasedSchemaStores.forEach {  fileBasedSchemaStore ->
         fileBasedSchemaStore.sourcesChanged
            .subscribe { message ->
               when (val result: Either<CompilationException, Schema> = schemaPublisher.submitSchemas(message.sources)) {
                  is Either.Left -> logger.warn { "Update of sources resulted in ${result.a.errors.errors().size} compilation errors." }
                  is Either.Right -> logger.info { "Sources updated successfully" }
               }

            }

      }
   }

   fun refreshAllSources() {
      fileBasedSchemaStores.forEach { refreshSources(it) }
   }

   fun refreshSources(fileBasedSchemaStore:FileSystemSchemaRepository) =
      fileBasedSchemaStore.refreshSources()


}
