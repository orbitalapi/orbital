package com.orbitalhq.schemaServer.core.adaptors.openapi

import com.orbitalhq.DefaultPackageMetadata
import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.asVersionedSource
import com.orbitalhq.schema.publisher.loaders.SchemaPackageTransport
import com.orbitalhq.schema.publisher.loaders.SchemaSourcesAdaptor
import com.orbitalhq.schema.publisher.loaders.SourceGenerator
import com.orbitalhq.schemaServer.packages.OpenApiPackageLoaderSpec
import com.orbitalhq.schemas.taxi.mergeLists
import com.orbitalhq.utils.orElse
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import lang.taxi.generators.openApi.GeneratorOptions
import lang.taxi.generators.openApi.TaxiGenerator
import lang.taxi.packages.SourcesType
import lang.taxi.packages.SourcesTypes
import lang.taxi.sources.SourceCodeLanguages
import mu.KotlinLogging
import org.jetbrains.kotlinx.dataframe.io.OpenApi
import reactor.core.publisher.Mono
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

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
         .flatMap { uri ->
            logger.debug { "Reading openAPI spec from $uri" }
            transport.readUri(uri).map { uri to it }
         }
         .map { (uri, openApiSpecBytes) ->
            val openApiSpec = String(openApiSpecBytes)
            val filename = uri.toASCIIString().substringAfterLast('/')
            val versionedSource = VersionedSource(
               name = filename,
               version = VersionedSource.DEFAULT_VERSION.toString(),
               content = openApiSpec,
               language = SourceCodeLanguages.OAS,
               path = uri.toASCIIString(),
            )
            OpenApiSourceGenerator(FixedOpenApiSpecProvider(spec))
               .generateSourcePackage(listOf(versionedSource), packageMetadata)
         }
   }
}

/**
 * SourceGenerator for OpenApi sources.
 * Has some limitations currently if invoked as a no-args source generator (eg., when
 * transpiling additionalSources in a Taxi project, versus when loading an OpenAPI spec as a project).
 *
 * Specifically - the defaultNamespace becomes the package's group Id, and no support for a service basePath.
 * We can look to enrich this later.
 */
open class OpenApiSourceGenerator(
   private val specProvider: OpenApiSpecProvider
) : SourceGenerator {
   companion object {
      const val OPENAPI_SOURCES_TYPE = "@orbital/openapi"
      private val logger = KotlinLogging.logger {}
   }

   override fun supportsSources(sourcesType: SourcesType): Boolean {
      return sourcesType == OPENAPI_SOURCES_TYPE
   }

   override fun generateSourcePackage(
      sourceFiles: List<VersionedSource>,
      packageMetadata: PackageMetadata
   ): SourcePackage {
      val taxiSourcesAndSourceMaps = sourceFiles.map { openApiSource ->
         val spec = specProvider.provide(openApiSource, packageMetadata)
         val generatedTaxiCode = TaxiGenerator().generateAsStrings(
            openApiSource.content,
            spec.defaultNamespace,
            GeneratorOptions(
               serviceBasePath = spec.serviceBasePath
            )
         )
         logger.info { "Converted OpenAPI spec from ${openApiSource.path.orElse(openApiSource.name)} to ${generatedTaxiCode.taxi.size} taxi sources with ${generatedTaxiCode.errorCount} errors and ${generatedTaxiCode.warningCount}" }
         val generatedTaxi =
            generatedTaxiCode.asVersionedSource(packageMetadata.identifier, "GeneratedFrom_${openApiSource.name}")
         val sourceMap = mapOf(
            SourcesTypes.ORIGINAL_SOURCE to listOf(
               VersionedSource(
                  openApiSource.name,
                  packageMetadata.identifier.version,
                  openApiSource.content,
                  SourceCodeLanguages.OAS,
                  openApiSource.path
               )
            )
         )
         generatedTaxi to sourceMap
      }
      val generatedTaxiSources = taxiSourcesAndSourceMaps.flatMap { it.first }
      val originalSources = taxiSourcesAndSourceMaps.map { it.second }
         .reduceOrNull { acc, map -> acc.mergeLists(map) }
         ?: emptyMap()

      val generatedSourcePackage = SourcePackage(
         packageMetadata,
         generatedTaxiSources,
         additionalSources = originalSources
      )
      return generatedSourcePackage
   }
}

/**
 * Simple interface that determines how the values from the spec are provided.
 * When we're working against an OpenAPI project (defined in a workspace.conf), then the
 *
 */
sealed interface OpenApiSpecProvider {
   fun provide(source: VersionedSource, packageMetadata: PackageMetadata): OpenApiPackageLoaderSpec
}

class FixedOpenApiSpecProvider(private val spec: OpenApiPackageLoaderSpec) : OpenApiSpecProvider {
   override fun provide(source: VersionedSource, packageMetadata: PackageMetadata): OpenApiPackageLoaderSpec = spec
}

class FileLoadingOpenApiSpecProvider() : OpenApiSpecProvider {
   override fun provide(source: VersionedSource, packageMetadata: PackageMetadata): OpenApiPackageLoaderSpec {
      require(source.path != null) { "Cannot load OpenAPI spec for ${source.name} as no path was provided" }
      val uri = URI.create(source.path)
      val isFile = (uri.scheme == "file" || uri.scheme == null)
      require(isFile) { "Reading OpenAPI specs in taxi projects is only supported on file-based URIs - found ${uri.toASCIIString()}" }
      val sourceFile = Paths.get(source.path)
      val configFileName = "${sourceFile.toFile().nameWithoutExtension}.taxi.conf"
      val configFilePath = sourceFile.parent.resolve(configFileName)
      return if (Files.exists(configFilePath)) {
         readPackageLoaderSpec(configFilePath, packageMetadata)
      } else {
         error("OpenAPI spec files require a corresponding config file defining defaultNamespace and optionally serviceBasePath. File is missing at $configFilePath")
      }
   }

   private fun readPackageLoaderSpec(configFilePath: Path, packageMetadata: PackageMetadata): OpenApiPackageLoaderSpec {
      val config = ConfigFactory.parseFile(configFilePath.toFile())
         .resolve()
      val defaultNamespace = config.extract<String>(OpenApiPackageLoaderSpec::defaultNamespace.name)
      val serviceBasePath = config.extract<String?>(OpenApiPackageLoaderSpec::serviceBasePath.name)
      return OpenApiPackageLoaderSpec(
         identifier = packageMetadata.identifier,
         uri = null,
         defaultNamespace = defaultNamespace,
         serviceBasePath = serviceBasePath
      )
   }

}
