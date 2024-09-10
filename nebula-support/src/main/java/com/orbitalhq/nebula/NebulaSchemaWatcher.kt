package com.orbitalhq.nebula

import com.google.common.collect.MapDifference.ValueDifference
import com.google.common.collect.Maps
import com.orbitalhq.VersionedSource
import com.orbitalhq.config.ConfigFileLocationConventions
import com.orbitalhq.schema.consumer.SchemaChangedEventProvider
import com.orbitalhq.schema.consumer.SchemaConfigSourceLoader
import mu.KotlinLogging
import reactor.core.publisher.Flux

/**
 * Uses a SchemaConfigSourceLoader to watch for changes
 * to Nebula stacks, and emits NebulaStacksChangedEvent on a flux
 */
class NebulaSchemaWatcher(
   loader: SchemaConfigSourceLoader,
) {
   private var lastSourcesState: Map<String, VersionedSource> = emptyMap()

   companion object {
      private val logger = KotlinLogging.logger {}
      fun fromEventSource(schemaChangedEventProvider: SchemaChangedEventProvider): NebulaSchemaWatcher {
         return NebulaSchemaWatcher(
            SchemaConfigSourceLoader(
               schemaChangedEventProvider,
               "*.nebula.kts",
               ConfigFileLocationConventions.OrbitalNebulaKey
            )
         )
      }
   }

   val stacksUpdated:Flux<NebulaStacksChangedEvent>

   init {
      logger.info { "Nebula Schema Watcher started" }
      stacksUpdated = loader.contentUpdated.mapNotNull { _ ->
         val sourcePackages = loader.load()
         val nebulaSources = sourcePackages.flatMap { it.sources }.associateBy { it.packageQualifiedName }
         val differences = Maps.difference(nebulaSources, lastSourcesState)
         logger.info { "Schema updated - schema currently has a total of ${nebulaSources.size} Nebula files" }
         val updateEvent = if (!differences.areEqual()) {
            val event = NebulaStacksChangedEvent(
               added = differences.entriesOnlyOnLeft(),
               removed = differences.entriesOnlyOnRight(),
               updated = differences.entriesDiffering()
            )
            logger.info(event.toString())
            event
         } else null
         lastSourcesState = nebulaSources
         updateEvent
      }
   }
}

data class NebulaStacksChangedEvent(
   val added: Map<String,VersionedSource>,
   val removed: Map<String,VersionedSource>,
   val updated: Map<String,ValueDifference<VersionedSource>>
) {
   override fun toString(): String = "NebulaStacksChangedEvent - ${added.size} added; ${removed.size} removed; ${updated.size} updated"
}

