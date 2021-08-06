package io.vyne.schemaServer.openapi

import com.github.zafarkhaja.semver.Version
import io.vyne.VersionedSource
import io.vyne.schemaServer.CompilerService
import io.vyne.schemaServer.VersionedSourceLoader
import lang.taxi.generators.openApi.TaxiGenerator
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.net.URI

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
         val sources = versionedSourceLoader.loadVersionedSources(false)
         compilerService.recompile(sources)
      }
   }
}

class OpenApiVersionedSourceLoader(
   val name: String,
   val url: URI,
   private val defaultNamespace: String,
) : VersionedSourceLoader {

   private val logger = KotlinLogging.logger {}

   override fun loadVersionedSources(incrementVersion: Boolean): List<VersionedSource> {
      val openApiSpec = url.toURL().readText()
      val taxiDef =  TaxiGenerator().generateAsStrings(openApiSpec, defaultNamespace)
      if (taxiDef.messages.isNotEmpty()) {
         logger.warn { "$name - $url returned warnings: ${taxiDef.messages.joinToString("\n")}" }
      }
      logger.info { "Retrieved ${taxiDef.taxi.size} taxi documents from $name - $url"}
      return taxiDef.taxi.map { VersionedSource(name, Version.valueOf("0.1.0").toString(), taxiDef.taxi.joinToString("\n")) }
   }
}

interface OpenApiSchemaSource {
   fun getSpec(): String
   fun specChanged(): Boolean
}
