package io.vyne.schemaServer

import io.vyne.SchemaId
import io.vyne.VersionedSource
import io.vyne.schemaStore.SchemaPublisher
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

@Component
class CompilerService(
   @Suppress("SpringJavaInjectionPointsAutowiringInspection")
   private val schemaPublisher: SchemaPublisher,
) {
   private val logger = KotlinLogging.logger {}
   private val sources: ConcurrentMap<SchemaId, VersionedSource> = ConcurrentHashMap()

   fun recompile(newSources: List<VersionedSource>) {

      newSources.forEach { source ->
         sources[source.id] = source
      }

      if (sources.isNotEmpty()) {
         logger.info("Recompiling ${sources.size} files")
         schemaPublisher.submitSchemas(sources.values.toList())
      } else {
         logger.warn("No sources were found. I'll just wait here.")
      }
   }
}
