package com.orbitalhq.schema.consumer

import com.orbitalhq.SourcePackage
import com.orbitalhq.config.ConfigSourceLoader
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

abstract class BaseCachingConfigLoader(
   /**
    * Generally, this is the name of the config file you want to load (eg., auth.conf).
    * However, can also be a file pattern (eg *.pipeline.json) to support
    * loading multiple files
    */
   private val filePattern: String,
):ConfigSourceLoader {
   private val sink = Sinks.many().multicast().directBestEffort<Class<out ConfigSourceLoader>>()

   companion object {
      private val logger = KotlinLogging.logger {}

      object CacheKey
   }

   private val contentCache = ConcurrentHashMap<CacheKey, List<SourcePackage>>()

   override val contentUpdated: Flux<Class<out ConfigSourceLoader>>
      get() = sink.asFlux()

   /**
    * Populates the cache.
    * Be sure to pass the correct "additionalSources" entry, not the schema's actual source
    */
   protected fun buildConfigSourcesCache(sources: List<SourcePackage>) {
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
      val hoconSources = sources.map { sourcePackage ->
         val requestedSources = sourcePackage.sources
            .filter { pathMatcher.matches(Paths.get(it.name)) }
         val filteredSourcePackage = sourcePackage.copy(sources = requestedSources)
         logger.info { "Package ${sourcePackage.identifier.id} contains ${filteredSourcePackage.sources.size} sources for pattern $pathGlob" }
         filteredSourcePackage
      }
      contentCache[CacheKey] = hoconSources
      sink.emitNext(this::class.java, Sinks.EmitFailureHandler.FAIL_FAST)
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

}
