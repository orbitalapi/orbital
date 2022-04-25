package io.vyne.schemaServer

import io.vyne.VersionedSource
import io.vyne.schema.publisher.SchemaPublisherTransport
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

// Merge conclift notes:
// This file was deleted on the jdbc-connector branch,
// but contains changes on develop, from supporting
// multiple sources.
// Need to resolve this.
@Component
@Deprecated("Replaced by LocalFileSchemaPublisher, which defers compilation to a SchemaPublisher")
class CompilerService(
   @Suppress("SpringJavaInjectionPointsAutowiringInspection")
   private val schemaPublisher: SchemaPublisherTransport,
) {
   private val logger = KotlinLogging.logger {}
   @Volatile
   private var sources: ConcurrentMap<String, List<VersionedSource>> = ConcurrentHashMap()

   fun recompile(newSources: Map<String, List<VersionedSource>>) {
      sources = ConcurrentHashMap(newSources)
      recompile()
   }

   fun recompile(identifier: String, newSources: List<VersionedSource>) {
      val previous = sources.put(identifier, newSources)
      if (previous != newSources) {
         recompile()
      }
   }

   private fun recompile() {
      val allSources = sources.sortedValues().flatten()

      if (allSources.isNotEmpty()) {
         logger.info("Recompiling ${allSources.size} files")
         schemaPublisher.submitSchemas(allSources)
      } else {
         logger.warn("No sources were found. I'll just wait here.")
      }
   }

   private fun <K : Comparable<K>, V> Map<K, V>.sortedValues() =
      entries
         .sortedBy { it.key }
         .map { it.value }
}
