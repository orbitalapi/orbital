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

class SchemaConfigSourceLoader(
   schemaEventSource: SchemaChangedEventProvider,
   /**
    * Generally, this is the name of the config file you want to load (eg., auth.conf).
    * However, can also be a file pattern (eg *.pipeline.json) to support
    * loading multiple files
    */
   private val filePattern: String,
   private val sourceType: SourcesType = "@orbital/config"
) : ConfigSourceLoader {

   private val sink = Sinks.many().multicast().directBestEffort<Class<out ConfigSourceLoader>>()

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
      // This is a hack, and should find a tidier way.
      // Need to support passing a filename - eg: auth.conf,
      // which should match /a/b/c/auth.conf and auth.conf
      // However, also want to support passing *.conf, which should support /a/b/c/foo.conf and foo.conf
      // So, expanding *.conf to **.conf and auth.conf to **auth.conf.
      // This is a hack, but there's test coverage, so feel free to improve.
      val pathGlob = if (filePattern.startsWith("*")) {
         "glob:*$filePattern"
      } else {
         "glob:**$filePattern"
      }
      val pathMatcher = Paths.get(filePattern).fileSystem.getPathMatcher(pathGlob)
      val filename = Paths.get(filePattern).fileName.toString()
      val sources = schema.additionalSources[sourceType] ?: emptyList()
      val hoconSources = sources.map { sourcePackage ->
         val requestedSources = sourcePackage.sources
            .filter { pathMatcher.matches(Paths.get(it.name)) }
         sourcePackage.copy(sources = requestedSources)
      }
      contentCache[CacheKey] = hoconSources
      sink.emitNext(SchemaConfigSourceLoader::class.java, Sinks.EmitFailureHandler.FAIL_FAST)
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

   override val contentUpdated: Flux<Class<out ConfigSourceLoader>>
      get() = sink.asFlux()
}
