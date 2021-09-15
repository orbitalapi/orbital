package io.vyne.schemaServer.openapi

import com.github.zafarkhaja.semver.Version
import io.vyne.VersionedSource
import io.vyne.schemaServer.CompilerService
import io.vyne.schemaServer.VersionedSourceLoader
import io.vyne.utils.readString
import io.vyne.utils.throwUnrecoverable
import lang.taxi.generators.openApi.TaxiGenerator
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.net.URI
import java.time.Duration

@Component
class OpenApiWatcher(
   val versionedSourceLoaders: List<OpenApiVersionedSourceLoader>,
   val compilerService: CompilerService,
) {

   private val logger = KotlinLogging.logger {}

   @Scheduled(fixedRateString = "\${taxi.openApiPollPeriodMs:300000}")
   fun actOnChanges() {
      versionedSourceLoaders.forEach { versionedSourceLoader ->
         logger.info { "Starting scheduled poll of ${versionedSourceLoader.name} - ${versionedSourceLoader.url}" }
         try {
            val sources = versionedSourceLoader.loadVersionedSources(false)
            compilerService.recompile(versionedSourceLoader.identifier, sources)
         } catch (e: Exception) {
            throwUnrecoverable(e)
            logger.warn(e) { "Failed to retrieve openapi for ${versionedSourceLoader.name} - ${versionedSourceLoader.identifier}" }
         }
      }
   }
}

class OpenApiVersionedSourceLoader(
   val name: String,
   val url: URI,
   private val defaultNamespace: String,
   private val connectTimeout: Duration,
   private val readTimeout: Duration,
) : VersionedSourceLoader {

   private val logger = KotlinLogging.logger {}

   override val identifier: String = name

   override fun loadVersionedSources(incrementVersion: Boolean): List<VersionedSource> {
      val openApiSpec = url.toURL().readString {
         connectTimeout = this@OpenApiVersionedSourceLoader.connectTimeout.toMillis().toInt()
         readTimeout = this@OpenApiVersionedSourceLoader.readTimeout.toMillis().toInt()
      }
      val taxiSource = generateTaxiCode(openApiSpec)
      return listOf(
         VersionedSource(
            name,
            Version.valueOf("0.1.0").toString(),
            taxiSource
         )
      )
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
