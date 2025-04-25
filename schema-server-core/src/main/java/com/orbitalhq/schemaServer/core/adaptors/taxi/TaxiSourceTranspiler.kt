package com.orbitalhq.schemaServer.core.adaptors.taxi

import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.schema.publisher.loaders.SourceGenerator
import com.orbitalhq.schemaServer.core.adaptors.avro.AvroTaxiSourceGenerator
import com.orbitalhq.schemaServer.core.adaptors.openapi.FileLoadingOpenApiSpecProvider
import com.orbitalhq.schemaServer.core.adaptors.openapi.OpenApiSourceGenerator
import com.orbitalhq.schemaServer.core.adaptors.protobuf.ProtobufTaxiSourceGenerator
import com.orbitalhq.schemas.taxi.mergeLists

/**
 * Converts sources declared in additionalSources of a source package
 * and converts them to taxi, using SourceGenerator
 */
class TaxiSourceTranspiler(
   private val sourceGenerators:List<SourceGenerator> = DEFAULT_SOURCE_GENERATORS
) {
   companion object {
      val DEFAULT_SOURCE_GENERATORS = listOf(
         AvroTaxiSourceGenerator(),
         OpenApiSourceGenerator(FileLoadingOpenApiSpecProvider()),
         ProtobufTaxiSourceGenerator()
      )
   }

   fun transpileAndCombineSources(sourcePackage: SourcePackage): SourcePackage {
      val transpiledPackages = transpileAdditionalSources(sourcePackage)
      return combineSourcePackages(sourcePackage, transpiledPackages)
   }

   private fun transpileAdditionalSources(
      sourcePackage: SourcePackage
   ): List<SourcePackage> {
      return sourcePackage.additionalSources
         .mapNotNull { (sourceType, sources) ->
            val generator = sourceGenerators.firstOrNull {
               it.supportsSources(sourceType)
            } ?: return@mapNotNull null
            generator.generateSourcePackage(sources, sourcePackage.packageMetadata)
         }
   }
   private fun combineSourcePackages(primarySourcePackage: SourcePackage, otherSourcePackages: List<SourcePackage>):SourcePackage {
      if (otherSourcePackages.isEmpty()) {
         return primarySourcePackage
      }
      return otherSourcePackages.fold(primarySourcePackage) { a,b ->
         val mergedSources = a.sources + b.sources
         val mergedAdditionalSources = a.additionalSources.mergeLists(b.additionalSources)
         a.copy(
            sources = mergedSources,
            additionalSources = mergedAdditionalSources
         )
      }
   }
}
