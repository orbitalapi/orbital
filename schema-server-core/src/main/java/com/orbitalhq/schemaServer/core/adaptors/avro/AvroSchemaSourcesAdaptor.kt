package com.orbitalhq.schemaServer.core.adaptors.avro

import com.orbitalhq.DefaultPackageMetadata
import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.asVersionedSource
import com.orbitalhq.schema.publisher.loaders.SchemaPackageTransport
import com.orbitalhq.schema.publisher.loaders.SchemaSourcesAdaptor
import com.orbitalhq.schema.publisher.loaders.SourceGenerator
import com.orbitalhq.schemaServer.packages.AvroPackageLoaderSpec
import lang.taxi.generators.SourceMap
import lang.taxi.generators.avro.AvroSchemaFormats
import lang.taxi.generators.avro.TaxiGenerator
import lang.taxi.packages.CompilerOptions
import lang.taxi.packages.SourcesType
import lang.taxi.sources.SourceCodeLanguages
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Instant


class AvroSchemaSourcesAdaptor(private val spec: AvroPackageLoaderSpec) : AvroTaxiSourceGenerator(), SchemaSourcesAdaptor {
   override fun buildMetadata(transport: SchemaPackageTransport): Mono<PackageMetadata> {
      return Mono.just(
         DefaultPackageMetadata(
            spec.identifier,
            submissionDate = Instant.now(),
            dependencies = emptyList(),
            compilerOptions = spec.compilerOptions
         )
      )
   }

   private fun getAvroUris(transport: SchemaPackageTransport): Flux<URI> {
      return transport.listUris()
         .filter { uri -> AvroSchemaFormats.isSupportedFormat(uri) }
   }

   override fun convert(packageMetadata: PackageMetadata, transport: SchemaPackageTransport): Mono<SourcePackage> {
      return getAvroUris(transport)
         .flatMap { uri -> transport.readUri(uri).map { uri to it } }
         .map { (uri, avroSchemaBytes) ->
            val avroSchema = String(avroSchemaBytes)
            val fileName = uri.path.substringAfterLast("/").ifEmpty {
               uri.toURL().file
            }
            VersionedSource(
               name = fileName,
               version = packageMetadata.identifier.version,
               content = avroSchema,
               language = SourceCodeLanguages.AVRO,
               path = uri.toASCIIString()
            )
         }.collectList()
         .map { avroSourceFiles ->
            generateSourcePackage(sourceFiles = avroSourceFiles, packageMetadata)
         }
   }
}

// Design choice: Have split this out to a separate class
// as AvroSchemaSourcesAdaptor requires a spec in it's constructor,
// and this class is focussed purely on converting one source type to another.
open class AvroTaxiSourceGenerator : SourceGenerator {
   companion object {
      val AVRO_SOURCES_TYPE = "@orbital/avro"
   }
   override fun supportsSources(sourcesType: SourcesType): Boolean {
      return sourcesType == AVRO_SOURCES_TYPE
   }

   override fun generateSourcePackage(
      sourceFiles: List<VersionedSource>,
      packageMetadata: PackageMetadata
   ): SourcePackage {
      val taxiSource = sourceFiles.map { avroSourceFile ->
         val generatedTaxiCode = TaxiGenerator().generate(avroSourceFile.content, avroSourceFile.name)
         generatedTaxiCode.sourceMap to generatedTaxiCode.asVersionedSource(
            packageMetadata.identifier,
            "GeneratedFrom_${avroSourceFile.name}"
         )
      }
      val allTaxiSources = taxiSource.flatMap { it.second }

      val sourceMap = taxiSource.map { it.first }
         .reduceOrNull { acc, sourceMap -> acc.combine(sourceMap) }
         ?: SourceMap.EMPTY
      return SourcePackage.asTranspiledPackage(
         packageMetadata,
         sourceFiles,
         allTaxiSources,
         sourceMap
      )
   }

}
