package io.vyne.schemaServer.publisher

import arrow.core.Either
import io.vyne.VersionedSource
import io.vyne.schemaPublisherApi.SchemaPublisher
import io.vyne.schemaPublisherApi.VersionedSourceSubmission
import io.vyne.schemaServer.UpdatingVersionedSourceLoader
import io.vyne.schemaServer.VersionedSourceLoader
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
   val sourceLoaders: List<VersionedSourceLoader>,
   val schemaPublisher: SchemaPublisher
) {

   init {
      sourceLoaders.filterIsInstance<UpdatingVersionedSourceLoader>()
         .forEach { sourceLoader ->
            logger.info { "Listening for changes from source loader ${sourceLoader.identifier}" }
            sourceLoader.sourcesChanged.subscribe { message ->
               submitSources(message.sources)
            }
         }
   }

   fun refreshAllSources(): List<VersionedSource> {
      val allSources = sourceLoaders.map { it.loadVersionedSources() }
      allSources.forEach { submitSources(it) }
      return allSources.flatMap { it.sources }
   }

   fun submitSources(sources: VersionedSourceSubmission): Either<CompilationException, Schema> {
      val result: Either<CompilationException, Schema> = schemaPublisher.submitSchemaPackage(sources)
      when (result) {
         is Either.Left -> logger.warn { "Update of sources resulted in ${result.a.errors.errors().size} compilation errors." }
         is Either.Right -> logger.info { "Sources updated successfully" }
      }
      return result
   }
}
