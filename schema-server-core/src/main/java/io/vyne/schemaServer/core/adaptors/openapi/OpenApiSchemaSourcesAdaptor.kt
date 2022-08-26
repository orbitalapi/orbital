package io.vyne.schemaServer.core.adaptors.openapi

import io.vyne.PackageIdentifier
import io.vyne.PackageMetadata
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.schema.publisher.loaders.SchemaPackageTransport
import io.vyne.schema.publisher.loaders.SchemaSourcesAdaptor
import io.vyne.schemaServer.core.adaptors.OpenApiPackageLoaderSpec
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
            val version = TaxiGenerator().readSchemaVersion(openApiSpec)
            OpenApiSpecPackageMetadata(
               PackageIdentifier(
                  spec.identifier.organisation,
                  spec.identifier.name,
                  version ?: spec.identifier.version
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
         }
      ))
   }
}
