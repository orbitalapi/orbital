package io.vyne.schemaServer.core.openApi

import com.github.zafarkhaja.semver.Version
import com.google.common.cache.CacheBuilder
import io.vyne.VersionedSource
import io.vyne.schemaServer.core.VersionedSourceLoader
import io.vyne.schemaServer.core.publisher.SourceWatchingSchemaPublisher
import io.vyne.utils.readString
import io.vyne.utils.throwUnrecoverable
import lang.taxi.generators.openApi.TaxiGenerator
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.net.URI
import java.time.Duration
import java.util.concurrent.ExecutionException

@Component
class OpenApiWatcher(
   val versionedSourceLoaders: List<OpenApiVersionedSourceLoader>,
   val schemaPublisher: SourceWatchingSchemaPublisher
) {

   private val logger = KotlinLogging.logger {}
   private var sources: Map<String, Set<VersionedSource>> = mapOf()

   @Scheduled(fixedRateString = "\${open-api.pollFrequency:PT300S}")
   fun pollForUpdates() {
      val loadedSources = mutableMapOf<String, Set<VersionedSource>>()
      versionedSourceLoaders.forEach { versionedSourceLoader ->
         logger.info { "Starting scheduled poll of ${versionedSourceLoader.name} - ${versionedSourceLoader.url}" }
         try {
            // On our poll schedule we've been configured to explicitly call the remote service,
            // so don't permit cached values.  In other scenarios (ie., when rebuilding sources because another file
            // elsewhere has changed), then cached values are fine.
            val sources = versionedSourceLoader.loadVersionedSources(cachedValuePermissible = false)
            loadedSources[versionedSourceLoader.identifier] = sources.toSet()
         } catch (e: Exception) {
            throwUnrecoverable(e)
            logger.warn(e) { "Failed to retrieve openapi for ${versionedSourceLoader.name} - ${versionedSourceLoader.identifier}" }
         }
      }
      if (sources != loadedSources) {
         schemaPublisher.submitSources(loadedSources.values.flatten())
         this.sources = loadedSources.toMap()
      }
   }
}

class OpenApiVersionedSourceLoader(
   val name: String,
   val url: URI,
   private val defaultNamespace: String,
   private val connectTimeout: Duration,
   private val readTimeout: Duration,
) : io.vyne.schemaServer.core.VersionedSourceLoader {

   private val logger = KotlinLogging.logger {}
   private val cache = CacheBuilder.newBuilder()
      .build<URI, List<VersionedSource>>()
   override val identifier: String = name


   /**
    * Loads sources.
    * This is an expensive operation, as a remote call happens to fetch the data.
    * Therefore, generally speaking cached results are used where possible.
    *
    * On poll schedules, we should actually call the remote service (since that's
    * the configuration contract), so pass cachedValuePermissible = false.
    *
    * On all other scenarios (such as providing sources to recompile on a general refresh),
    * use cached sources
    */
   override fun loadVersionedSources(
      forceVersionIncrement: Boolean,
      cachedValuePermissible: Boolean
   ): List<VersionedSource> {
      if (!cachedValuePermissible) {
         cache.invalidate(url)
      }

      return try {
         cache.get(url) {
            val openApiSpec = url.toURL().readString {
               connectTimeout = this@OpenApiVersionedSourceLoader.connectTimeout.toMillis().toInt()
               readTimeout = this@OpenApiVersionedSourceLoader.readTimeout.toMillis().toInt()
            }
            val taxiSource = generateTaxiCode(openApiSpec)
            listOf(
               VersionedSource(
                  name,
                  Version.valueOf("0.1.0").toString(),
                  taxiSource
               )
            )
         }
      } catch (e: ExecutionException) {
         // Unwrap the inner exception, since it's more meaningful.
         throw e.cause ?: e
      }

   }

   private fun generateTaxiCode(openApiSpec: String): String {
      val taxiDef = TaxiGenerator().generateAsStrings(openApiSpec, defaultNamespace)
      if (taxiDef.messages.isNotEmpty()) {
         val warnings = taxiDef.messages.joinToString("\n")
         logger.warn { "$name - $url returned warnings: $warnings" }
      }
      logger.info { "Retrieved ${taxiDef.taxi.size} taxi documents from $name - $url" }
      return taxiDef.taxi.joinToString("\n\n")
   }
}
