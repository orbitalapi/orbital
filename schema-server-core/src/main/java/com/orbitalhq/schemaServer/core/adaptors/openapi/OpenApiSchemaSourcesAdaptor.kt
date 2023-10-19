package com.orbitalhq.schemaServer.core.adaptors.openapi

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.schema.api.SchemaPackageTransport
import com.orbitalhq.schema.api.SchemaSourcesAdaptor
import com.orbitalhq.schemaServer.packages.OpenApiPackageLoaderSpec
import lang.taxi.generators.openApi.GeneratorOptions
import lang.taxi.generators.openApi.TaxiGenerator
import mu.KotlinLogging
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Instant

data class OpenApiSpecPackageMetadata(
   override val identifier: PackageIdentifier,

   /**
    * The date that this packageMetadata was considered 'as-of'.
    * In the case that two packages with the same identifier are submitted,
    * the "latest" wins - using this data to determine latest.
    */
   override val submissionDate: Instant,
   override val dependencies: List<PackageIdentifier>,
   val openApiSpec: String
) : PackageMetadata

class OpenApiSchemaSourcesAdaptor(private val spec: OpenApiPackageLoaderSpec) : SchemaSourcesAdaptor {
   private val logger = KotlinLogging.logger {}
   override fun buildMetadata(transport: SchemaPackageTransport): Mono<PackageMetadata> {

      return getOpenApiSpecUri(transport)
         .flatMap { uri ->
            transport.readUri(uri)
         }.map { byteArray ->
            val openApiSpec = String(byteArray)
            // Use the version fom the OpenApi spec if available.
//            val version = TaxiGenerator().readSchemaVersion(openApiSpec)
            OpenApiSpecPackageMetadata(
               PackageIdentifier(
                  spec.identifier.organisation,
                  spec.identifier.name,
                  // Used to use the version from the OpenApi spec if available, rather than
                  // the version in the spec.
                  // However, that breaks comparisons against specs and has a bunch
                  // of unexpected consequences.
                  spec.identifier.version
               ),
               spec.submissionDate,
               spec.dependencies,
               openApiSpec
            )
         }
   }

   private fun getOpenApiSpecUri(transport: SchemaPackageTransport): Mono<URI> {
      return transport.listUris()
         .collectList()
         .map { uris ->
            uris.singleOrNull()
               ?: error("There were ${uris.size} uris provided by the transport, and none specified in the config.  Unsure how to load the OpenApi spec.  Consider providing the uri directly in the config")
         }
   }

   override fun convert(packageMetadata: PackageMetadata, transport: SchemaPackageTransport): Mono<SourcePackage> {
      require(packageMetadata is OpenApiSpecPackageMetadata) { "Expected OpenApiSpecPackageMetadata, but got ${packageMetadata::class.simpleName}" }
      val generated = TaxiGenerator().generateAsStrings(
         packageMetadata.openApiSpec,
         spec.defaultNamespace,
         GeneratorOptions(
            serviceBasePath = spec.serviceBasePath
         )
      )
      return Mono.just(SourcePackage(
         packageMetadata,
         generated.taxi.mapIndexed { index, taxiSourceFile ->
            val suffix = if (index > 0) index.toString() else ""
            VersionedSource(
               packageMetadata.identifier.name + suffix,
               packageMetadata.identifier.version,
               taxiSourceFile
            )
         },
         emptyMap()
      ))
   }
}
