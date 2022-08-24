package io.vyne.schemaServer.core.publisher

import arrow.core.Either
import io.vyne.VersionedSource
import io.vyne.schema.publisher.SchemaPublisherTransport
import io.vyne.schemas.Schema
import lang.taxi.CompilationException
import lang.taxi.errors
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Watches local schema repositories, and publishes changes out to the
 * provided schema publisher.
 *
 * This class replaces the CompilerService, deferring compilation and validation to
 * the SchemaPublisher
 */
class SourceWatchingSchemaPublisher(
   val sourceLoaders: List<io.vyne.schemaServer.core.VersionedSourceLoader>,
   val schemaPublisher: SchemaPublisherTransport
) {

   init {
      sourceLoaders.filterIsInstance<io.vyne.schemaServer.core.UpdatingVersionedSourceLoader>()
         .forEach { sourceLoader ->
            logger.info { "Listening for changes from source loader ${sourceLoader.identifier}" }
            sourceLoader.sourcesChanged.subscribe { message ->
               submitSources(message.sources)
            }
         }
   }

   fun refreshAllSources(): List<VersionedSource> {
      val allSources = sourceLoaders.flatMap { it.loadVersionedSources() }
      submitSources(allSources)
      return allSources

   }

   fun submitSources(sources: List<VersionedSource>): Either<CompilationException, Schema> {
      val result: Either<CompilationException, Schema> = schemaPublisher.submitSchemas(sources)
      when (result) {
         is Either.Left -> logger.warn { "Update of sources resulted in ${result.value.errors.errors().size} compilation errors." }
         is Either.Right -> logger.info { "Sources updated successfully" }
      }
      return result
   }
}
