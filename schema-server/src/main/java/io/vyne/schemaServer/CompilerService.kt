package io.vyne.schemaServer

import io.vyne.schemaStore.SchemaPublisher
import mu.KLogger
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Component
class CompilerService(
   private val versionedSourceLoaders: List<VersionedSourceLoader>,
   private val schemaPublisher: SchemaPublisher,
   private val logger: KLogger = KotlinLogging.logger {},
) {

   fun recompile(incrementVersion: Boolean = true) {

      val sources = versionedSourceLoaders.flatMap { it.getSourcesFromFileSystem(incrementVersion) }

      if (sources.isNotEmpty()) {
         logger.info("Recompiling ${sources.size} files")
         schemaPublisher.submitSchemas(sources)
      } else {
         logger.warn("No sources were found in ${versionedSourceLoaders}. I'll just wait here.")
      }

   }
}

