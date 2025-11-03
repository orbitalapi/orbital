package com.orbitalhq.schemaServer.core.adaptors.soap

import com.orbitalhq.DefaultPackageMetadata
import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.asVersionedSource
import com.orbitalhq.schema.publisher.loaders.SchemaPackageTransport
import com.orbitalhq.schema.publisher.loaders.SchemaSourcesAdaptor
import com.orbitalhq.schema.publisher.loaders.SourceGenerator
import com.orbitalhq.schemaServer.core.adaptors.xsd.EmptyXsdSourceConfigProvider
import com.orbitalhq.schemaServer.core.adaptors.xsd.XsdSchemaSourceConfigProvider
import com.orbitalhq.schemaServer.packages.SoapPackageLoaderSpec
import lang.taxi.generators.GeneratedTaxiCode
import lang.taxi.generators.soap.SoapLanguage
import lang.taxi.generators.soap.TaxiGenerator
import lang.taxi.packages.SourcesType
import lang.taxi.sources.SourceCode
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI
import java.nio.file.Paths
import java.time.Instant
import kotlin.io.path.extension

class SoapSchemaSourcesAdaptor(
   private val spec: SoapPackageLoaderSpec,
   configProvider: XsdSchemaSourceConfigProvider = EmptyXsdSourceConfigProvider
) : SoapSchemaSourceGenerator(configProvider), SchemaSourcesAdaptor {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   override fun buildMetadata(transport: SchemaPackageTransport): Mono<PackageMetadata> {
      return Mono.just(
         DefaultPackageMetadata(
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
    * Creation defers to the SoapSchemaSourceGenerator, so there is consistent
    * code-gen for both using an adaptor (ie., when a project with a SOAP loader is declared),
    * and when loading SOAP sources within a Taxi Project.
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
            generateSourcePackage(sources, packageMetadata)
         }
   }
}

/**
 * This generator is used when a WSDL package is declared in the additional
 * sources of the Taxi source (versus as a SourceLoader for a SOAP based project
 * declared in a workspace.conf)
 *
 * This uses the newer approach of generating sources and attaching them
 * via a source map. The SoapSchemaSourcesAdaptor currently uses
 * the deprecated approach of not generating Taxi at this point. However, that
 * has been documented to have issues.
 *
 * After testing, we should move to using this approach in both cases.
 *
 */
open class SoapSchemaSourceGenerator(private val configProvider: XsdSchemaSourceConfigProvider = EmptyXsdSourceConfigProvider) :
   SourceGenerator {
   companion object {
      const val SOAP_SOURCES_TYPE = "@orbital/wsdl"
      private val logger = KotlinLogging.logger {}
   }

   override fun supportsSources(sourcesType: SourcesType): Boolean {
      return sourcesType == SOAP_SOURCES_TYPE
   }

   /**
    * Returns the sources, configured as WSDL sources.
    * They are not converted into
    */
   override fun generateSourcePackage(
      sourceFiles: List<VersionedSource>,
      packageMetadata: PackageMetadata
   ): SourcePackage {
      val wsdlSources = sourceFiles
         .filter { Paths.get(it.name).extension == "wsdl" }
      // Generate the taxi, and collect all the wsdl sources,
      // including any imported XSD's
      val (generatedTaxiCode, allWsdlSources) = wsdlSources
         .mapNotNull { wsdl ->
            TaxiGenerator().wsdlToGeneratedSources(wsdl.pathOrName.toUri())
         }.reduceOrNull { acc, pair ->
            val (accGeneratedTaxi, accSourceCode) = acc
            val (thisGeneratedTaxi, thisSourceCodes) = pair
            val mergedTaxi = accGeneratedTaxi.mergeWith(thisGeneratedTaxi)
            val mergedSources = accSourceCode + thisSourceCodes
            mergedTaxi to mergedSources
         } ?: kotlin.run {
         logger.warn { "After parsing the ${wsdlSources.size} provided WSDL sources in package ${packageMetadata.identifier}, no Taxi was generated. Returning an empty package" }
         GeneratedTaxiCode(emptyList(), emptyList()) to emptyList()
      }

      val allWsdlVersionSources = allWsdlSources.map { it.asVersionedSource() }
      return SourcePackage.asTranspiledPackage(
         packageMetadata = packageMetadata,
         originalSources = allWsdlVersionSources,
         generatedTaxiSources = generatedTaxiCode.asVersionedSource(packageMetadata.identifier, "GeneratedFromWsdl_"),
         sourceMap = generatedTaxiCode.sourceMap
      )
   }
}
