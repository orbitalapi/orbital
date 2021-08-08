package io.vyne.schemaServer

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
   @Volatile
   private var sources: ConcurrentMap<String, List<VersionedSource>> = ConcurrentHashMap()

   fun recompile(newSources: Map<String, List<VersionedSource>>) {
      sources = ConcurrentHashMap(newSources)
      recompile()
   }

   fun recompile(identifier: String, newSources: List<VersionedSource>) {
      sources[identifier] = newSources
      recompile()
   }

   private fun recompile() {
      val allSources = sources.values.flatten()

      if (allSources.isNotEmpty()) {
         logger.info("Recompiling ${allSources.size} files")
         schemaPublisher.submitSchemas(allSources)
      } else {
         logger.warn("No sources were found. I'll just wait here.")
      }
   }
}
