package io.vyne.schemaServer

import io.vyne.schemaStore.SchemaPublisher
import mu.KotlinLogging
import org.springframework.stereotype.Component

// Merge conclift notes:
// This file was deleted on the jdbc-connector branch,
// but contains changes on develop, from supporting
// multiple sources.
// Need to resolve this.
@Component
class CompilerService(
   private val versionedSourceLoaders: List<VersionedSourceLoader>,
   @Suppress("SpringJavaInjectionPointsAutowiringInspection")
   private val schemaPublisher: SchemaPublisher,
) {
   private val logger = KotlinLogging.logger {}

   fun recompile(incrementVersion: Boolean = true) {

      val sources = versionedSourceLoaders.flatMap { it.loadVersionedSources(incrementVersion) }

      if (sources.isNotEmpty()) {
         logger.info("Recompiling ${sources.size} files")
         schemaPublisher.submitSchemas(sources)
      } else {
         logger.warn("No sources were found in ${versionedSourceLoaders}. I'll just wait here.")
      }

   }
}

