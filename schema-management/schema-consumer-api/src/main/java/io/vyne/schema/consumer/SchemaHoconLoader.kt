package io.vyne.schema.consumer

import io.vyne.SourcePackage
import io.vyne.config.HoconLoader
import io.vyne.schemas.Schema
import lang.taxi.packages.SourcesType
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

class SchemaHoconLoader(
   schemaEventSource: SchemaChangedEventProvider,
   private val filename: String,
   private val sourceType: SourcesType = "@orbital/config"
) : HoconLoader {

   private val sink = Sinks.many().multicast().directBestEffort<Class<out HoconLoader>>()

   companion object {
      private val logger = KotlinLogging.logger {}

      object CacheKey
   }

   private val contentCache = ConcurrentHashMap<CacheKey, List<SourcePackage>>()

   init {
      Flux.from(schemaEventSource.schemaChanged)
         .subscribe { event ->
            val schema = event.newSchemaSet.schema
            load(schema)
         }
      if (schemaEventSource is SchemaStore) {
         load(schemaEventSource.schema())
      } else {
         logger.info { "Not loading initial schema, as provided event source is not a schema store.  Will wait for an update event" }
      }
   }

   private fun load(schema: Schema) {
      val filename = Paths.get(filename).fileName.toString()
      val sources = schema.additionalSources[sourceType] ?: emptyList()
      val hoconSources = sources.map { sourcePackage ->
         val requestedSources = sourcePackage.sources
            .filter { Paths.get(it.name).fileName.toString() == filename }
         sourcePackage.copy(sources = requestedSources)
      }
      contentCache[CacheKey] = hoconSources
      sink.emitNext(SchemaHoconLoader::class.java, Sinks.EmitFailureHandler.FAIL_FAST)
   }

   override fun load(): List<SourcePackage> {
      val loaded = contentCache[CacheKey]
      return if (loaded == null) {
         logger.warn { "The schema has not provided any updates yet." }
         emptyList()
      } else {
         loaded
      }
   }

   override val contentUpdated: Flux<Class<out HoconLoader>>
      get() = sink.asFlux()
}
