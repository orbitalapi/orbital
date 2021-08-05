package io.vyne.schemaServer

import io.vyne.schemaStore.SchemaPublisher
import mu.KLogger
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Component
final class CompilerService(
   @Value("\${taxi.schema-local-storage}") val projectHome: String,
   private val schemaPublisher: SchemaPublisher,
   private val logger: KLogger = KotlinLogging.logger {},
) {

   private val versionedSourceLoaders = listOf(
      FileSystemVersionedSourceLoader(projectHome)
   )

   fun recompile(incrementVersion: Boolean = true) {

      val sources = versionedSourceLoaders.flatMap { it.getSourcesFromFileSystem(incrementVersion) }

      if (sources.isNotEmpty()) {
         logger.info("Recompiling ${sources.size} files")
         schemaPublisher.submitSchemas(sources)
      } else {
         logger.warn("No sources were found at $projectHome. I'll just wait here.")
      }

   }
}

