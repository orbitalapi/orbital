package io.vyne.schemaServer.core.publisher

import arrow.core.Either
import io.vyne.SourcePackage
import io.vyne.schema.publisher.SchemaPublisherTransport
import io.vyne.schema.publisher.loaders.SchemaPackageTransport
import io.vyne.schemaServer.core.repositories.lifecycle.RepositoryLifecycleEventSource
import io.vyne.schemas.Schema
import lang.taxi.CompilationException
import lang.taxi.errors
import mu.KotlinLogging

/**
 * Watches local schema repositories, and publishes changes out to the
 * provided schema publisher.
 *
 * This class replaces the CompilerService, deferring compilation and validation to
 * the SchemaPublisher
 */
class SourceWatchingSchemaPublisher(
   private val schemaPublisher: SchemaPublisherTransport,
   private val eventSource: RepositoryLifecycleEventSource
) {
   private val logger = KotlinLogging.logger {}

   init {
      eventSource
         .sourcesChanged
         .subscribe { message ->
            logger.info { "Received source change message for packages ${message.packages.joinToString()}" }
            submitSources(message.packages)
         }
   }

//   fun refreshAllSources(): List<SourcePackage> {
//      val allSources = sourceLoaders.map { it.loadSourcePackage() }
//      submitSources(allSources)
//      return allSources
//   }

   private fun submitSources(sources: List<SourcePackage>): Either<CompilationException, Schema> {
      val result: Either<CompilationException, Schema> = schemaPublisher.submitPackages(sources)
      when (result) {
         is Either.Left -> logger.warn { "Update of sources resulted in ${result.value.errors.errors().size} compilation errors." }
         is Either.Right -> logger.info { "Sources updated successfully" }
      }
      return result
   }
}
