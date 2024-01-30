package com.orbitalhq.schema.consumer

import com.orbitalhq.SourcePackage
import com.orbitalhq.config.ConfigSourceLoader
import com.orbitalhq.schemas.Schema
import lang.taxi.packages.SourcesType
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

/**
 * Wraps a SchemaSource (eg, SchemaSet) and turns it into a ConfigSourceLoader.
 * As this class operates at the Schema level (which is already merged, and detatched from the underlying file system), it cannot support
 * writes.
 *
 * Note: Consider using a ProjectManagerConfigSourceLoader, which supports writes as well.
 */
class SchemaConfigSourceLoader(
   schemaEventSource: SchemaChangedEventProvider,
   /**
    * Generally, this is the name of the config file you want to load (eg., auth.conf).
    * However, can also be a file pattern (eg *.pipeline.json) to support
    * loading multiple files
    */
   private val filePattern: String,
   private val sourceType: SourcesType = "@orbital/config"
) : BaseCachingConfigLoader(filePattern) {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   init {
      Flux.from(schemaEventSource.schemaChanged)
         .subscribe { event ->
            val schema = event.newSchemaSet.schema
            load(schema)
         }

      // Load initial state
      if (schemaEventSource is SchemaStore) {
         load(schemaEventSource.schema())
      } else {
         logger.info { "Not loading initial schema, as provided event source is not a schema store.  Will wait for an update event" }
      }
   }


   private fun load(schema: Schema) {
      val sources = schema.additionalSources[sourceType] ?: emptyList()
      buildConfigSourcesCache(sources)
   }



}
