package com.orbitalhq.schemaServer.core.adaptors.protobuf

import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.asVersionedSource
import com.orbitalhq.protobuf.wire.RepoBuilder
import com.orbitalhq.schema.publisher.loaders.SourceGenerator
import lang.taxi.generators.protobuf.TaxiGenerator
import lang.taxi.packages.SourcesType

class ProtobufTaxiSourceGenerator : SourceGenerator {
   companion object {
      val PROTOBUF_SORUCES_TYPE = "@orbital/protobuf"
   }
   override fun supportsSources(sourcesType: SourcesType): Boolean = sourcesType == PROTOBUF_SORUCES_TYPE

   override fun generateSourcePackage(
      sourceFiles: List<VersionedSource>,
      packageMetadata: PackageMetadata
   ): SourcePackage {
      val protobufRepo = RepoBuilder()
      sourceFiles.forEach { sourceFile ->
         protobufRepo.add(sourceFile.name, sourceFile.content)
      }
      val protobufSchema = protobufRepo.schema()
      val generatedTaxi = TaxiGenerator()
         .generate(protobufSchema = protobufSchema)
      val taxiVersionedSource = generatedTaxi.asVersionedSource(packageMetadata.identifier, "GeneratedProtobuf")
      return SourcePackage.asTranspiledPackage(
         packageMetadata,
         sourceFiles,
         taxiVersionedSource,
         generatedTaxi.sourceMap
      )
   }
}
