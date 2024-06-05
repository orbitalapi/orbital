package com.orbitalhq.schemaServer.core.adaptors.openapi

import com.orbitalhq.DefaultPackageMetadata
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.asVersionedSource
import com.orbitalhq.schema.publisher.loaders.SchemaPackageTransport
import com.orbitalhq.schema.publisher.loaders.SchemaSourcesAdaptor
import com.orbitalhq.schemaServer.packages.OpenApiPackageLoaderSpec
import lang.taxi.generators.openApi.GeneratorOptions
import lang.taxi.generators.openApi.TaxiGenerator
import lang.taxi.packages.SourcesTypes
import lang.taxi.sources.SourceCodeLanguages
import mu.KotlinLogging
import reactor.core.publisher.Mono
import java.io.File
import java.net.URI
import java.time.Instant

class OpenApiSchemaSourcesAdaptor(private val spec: OpenApiPackageLoaderSpec) : SchemaSourcesAdaptor {
   private val logger = KotlinLogging.logger {}
   override fun buildMetadata(transport: SchemaPackageTransport): Mono<PackageMetadata> {

      return Mono.just(
         DefaultPackageMetadata(
            spec.identifier,
            spec.submissionDate,
            spec.dependencies
         )
      )
   }

   private fun getOpenApiSpecUri(transport: SchemaPackageTransport): Mono<URI> {
      return transport.listUris()
         .collectList()
         .map { uris ->
            val candidateFiles = uris.filter { !File(it).isDirectory }
            candidateFiles.singleOrNull()
               ?: error("There were ${uris.size} uris provided by the transport, and none specified in the config.  Unsure how to load the OpenApi spec.  Consider providing the uri directly in the config")
         }
   }

   override fun convert(packageMetadata: PackageMetadata, transport: SchemaPackageTransport): Mono<SourcePackage> {
      return getOpenApiSpecUri(transport)
         .flatMap { uri -> transport.readUri(uri).map { uri to it } }
         .map { (uri, openApiSpecBytes) ->
            val openApiSpec = String(openApiSpecBytes)
            val generatedTaxiCode = TaxiGenerator().generateAsStrings(
               openApiSpec, spec.defaultNamespace,
               GeneratorOptions(
                  serviceBasePath = spec.serviceBasePath
               )
            )
            logger.info { "Converted OpenAPI spec from ${uri.toASCIIString()} to ${generatedTaxiCode.taxi.size} taxi sources with ${generatedTaxiCode.errorCount} errors and ${generatedTaxiCode.warningCount}" }
            val fileName = uri.path.substringAfterLast("/").ifEmpty {
               uri.toURL().file
            }
            SourcePackage(
               packageMetadata,
               generatedTaxiCode.asVersionedSource(packageMetadata.identifier, "GeneratedFrom_${fileName}"),
               additionalSources = mapOf(
                  SourcesTypes.ORIGINAL_SOURCE to listOf(
                     VersionedSource(
                        fileName,
                        packageMetadata.identifier.version,
                        openApiSpec,
                        SourceCodeLanguages.OAS,
                        uri.toASCIIString()
                     )
                  )
               )
            )
         }
   }
}
