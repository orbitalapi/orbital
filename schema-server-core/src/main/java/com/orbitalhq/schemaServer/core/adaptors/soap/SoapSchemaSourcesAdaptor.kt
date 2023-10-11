package com.orbitalhq.schemaServer.core.adaptors.soap

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.schema.publisher.loaders.SchemaPackageTransport
import com.orbitalhq.schema.publisher.loaders.SchemaSourcesAdaptor
import com.orbitalhq.schemaServer.packages.SoapPackageLoaderSpec
import lang.taxi.generators.soap.SoapLanguage
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Instant

data class SoapPackageMetadata(
   override val identifier: PackageIdentifier,

   /**
    * The date that this packageMetadata was considered 'as-of'.
    * In the case that two packages with the same identifier are submitted,
    * the "latest" wins - using this data to determine latest.
    */
   override val submissionDate: Instant,
   override val dependencies: List<PackageIdentifier>,
) : PackageMetadata

class SoapSchemaSourcesAdaptor(private val spec: SoapPackageLoaderSpec) : SchemaSourcesAdaptor {
   companion object {
      private val logger = KotlinLogging.logger {}
   }
   override fun buildMetadata(transport: SchemaPackageTransport): Mono<PackageMetadata> {
      return Mono.just(
         SoapPackageMetadata(
            spec.identifier,
            submissionDate = Instant.now(),
            dependencies = emptyList()
         )
      )
   }

   private fun getWsdlUris(transport: SchemaPackageTransport): Flux<URI> {
      return transport.listUris()
         .filter { uri -> uri.toURL().file.endsWith("wsdl") }
   }

   /**
    * Loads the wsdls present in the package.
    * The wsdls aren't converted to taxi sources at this point,
    * instead we're using the new experimental SourceToTaxiLoader
    * approach, as converting wsdl-to-taxi, to then just reload the taxi
    * creates complications later, when the original wsdl is needed.
    */
   override fun convert(packageMetadata: PackageMetadata, transport: SchemaPackageTransport): Mono<SourcePackage> {
      return getWsdlUris(transport)
         .collectList()
         .map { uris ->
            val sources = uris.map { uri ->
               val wsdlContents = uri.toURL().readText()
               VersionedSource(
                  name = uri.toURL().file,
                  version = packageMetadata.identifier.version,
                  content = wsdlContents,
                  language = SoapLanguage.WSDL
               )
            }

            SourcePackage(
               packageMetadata,
               sources
            )
         }
   }
}
