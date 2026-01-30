package com.orbitalhq.schema.consumer

import com.orbitalhq.SourcePackage
import com.orbitalhq.config.ConfigSourceLoader
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.nio.file.FileSystems
import java.nio.file.InvalidPathException
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

abstract class BaseCachingConfigLoader(
   /**
    * Generally, this is the name of the config file you want to load (eg., auth.conf).
    * However, can also be a file pattern (eg *.pipeline.json) to support
    * loading multiple files
    */
   private val filePattern: String,
   /**
    * Optional environment name (eg., "preprod", "staging").
    * If provided, will also attempt to load environment-specific versions of config files.
    * For example, if filePattern is "auth.conf" and environmentName is "preprod",
    * will also load "auth.preprod.conf" if present.
    */
   private val environmentName: String? = null,
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
    * Generates an environment-specific file pattern.
    * For example: "auth.conf" with environment "preprod" becomes "auth.preprod.conf"
    *              "*.conf" with environment "preprod" becomes "*.preprod.conf"
    */
   private fun getEnvironmentSpecificPattern(pattern: String, env: String): String? {
      val lastDotIndex = pattern.lastIndexOf('.')
      return if (lastDotIndex > 0) {
         val beforeExtension = pattern.substring(0, lastDotIndex)
         val extension = pattern.substring(lastDotIndex)
         "$beforeExtension.$env$extension"
      } else {
         null
      }
   }

   /**
    * Populates the cache.
    * Be sure to pass the correct "additionalSources" entry, not the schema's actual source
    */
   protected fun buildConfigSourcesCache(sources: List<SourcePackage>):List<SourcePackage> {
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
      val pathMatcher = try {
         FileSystems.getDefault().getPathMatcher(pathGlob)
      } catch (e: InvalidPathException) {
         logger.error { "Cannot setup config loader ${this::class.simpleName} as the provided path $filePattern is invalid" }
         return emptyList()
      }

      // Also create matcher for environment-specific files if environment is specified
      val envPathMatcher = if (!environmentName.isNullOrEmpty()) {
         val envSpecificPattern = getEnvironmentSpecificPattern(filePattern, environmentName)
         if (envSpecificPattern != null) {
            val envPathGlob = if (envSpecificPattern.startsWith("*")) {
               "glob:*$envSpecificPattern"
            } else {
               "glob:**$envSpecificPattern"
            }
            try {
               FileSystems.getDefault().getPathMatcher(envPathGlob)
            } catch (e: InvalidPathException) {
               logger.warn { "Cannot create environment-specific path matcher for pattern $envSpecificPattern" }
               null
            }
         } else {
            null
         }
      } else {
         null
      }

      val hoconSources = sources.map { sourcePackage ->
         val requestedSources = sourcePackage.sources
            .filter { source ->
               val sourcePath = Paths.get(source.name)
               pathMatcher.matches(sourcePath) || (envPathMatcher?.matches(sourcePath) ?: false)
            }
         val filteredSourcePackage = sourcePackage.copy(sources = requestedSources)
         val matchInfo = if (envPathMatcher != null) {
            "patterns $pathGlob and environment-specific file"
         } else {
            "pattern $pathGlob"
         }
         logger.info { "Package ${sourcePackage.identifier.id} contains ${filteredSourcePackage.sources.size} sources for $matchInfo" }
         filteredSourcePackage
      }
      contentCache[CacheKey] = hoconSources
      sink.emitNext(this::class.java, Sinks.EmitFailureHandler.FAIL_FAST)
      return hoconSources
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
