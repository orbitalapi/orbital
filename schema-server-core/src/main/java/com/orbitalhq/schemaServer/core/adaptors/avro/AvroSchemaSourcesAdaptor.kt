package com.orbitalhq.schemaServer.core.adaptors.avro

import com.orbitalhq.DefaultPackageMetadata
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.asVersionedSource
import com.orbitalhq.schema.publisher.loaders.SchemaPackageTransport
import com.orbitalhq.schema.publisher.loaders.SchemaSourcesAdaptor
import com.orbitalhq.schemaServer.packages.AvroPackageLoaderSpec
import lang.taxi.generators.avro.AvroSchemaFormats
import lang.taxi.generators.avro.TaxiGenerator
import org.apache.commons.io.FilenameUtils
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Instant


class AvroSchemaSourcesAdaptor(private val spec: AvroPackageLoaderSpec) : SchemaSourcesAdaptor {
   override fun buildMetadata(transport: SchemaPackageTransport): Mono<PackageMetadata> {
      return Mono.just(
         DefaultPackageMetadata(
            spec.identifier,
            submissionDate = Instant.now(),
            dependencies = emptyList()
         )
      )
   }

   private fun getAvroUris(transport: SchemaPackageTransport): Flux<URI> {
      return transport.listUris()
         .filter { uri -> AvroSchemaFormats.isSupportedFormat(uri) }
   }

   override fun convert(packageMetadata: PackageMetadata, transport: SchemaPackageTransport): Mono<SourcePackage> {
      return getAvroUris(transport)
         .flatMap { uri -> transport.readUri(uri).map { uri to it} }
         .map { (uri, avroSchemaBytes) ->
            val avroSchema = String(avroSchemaBytes)
            val fileName = uri.path.substringAfterLast("/").ifEmpty {
               uri.toURL().file
            }
            VersionedSource(
               name = fileName,
               version = packageMetadata.identifier.version,
               content = avroSchema,
               language = FilenameUtils.getExtension(fileName),
               path = uri.toASCIIString()
            )
         }.collectList()
         .map { avroSourceFiles ->
            val taxiSource  = avroSourceFiles.flatMap { avroSourceFile ->
               val generatedTaxiCode = TaxiGenerator().generate(avroSourceFile.content)
               generatedTaxiCode.asVersionedSource(packageMetadata.identifier, "GeneratedFrom_${avroSourceFile.name}")
            }
            SourcePackage(packageMetadata, taxiSource,
               additionalSources = mapOf(
                  SourcePackage.ORIGINAL_SOURCE to avroSourceFiles
               )
            )
         }
   }
}
