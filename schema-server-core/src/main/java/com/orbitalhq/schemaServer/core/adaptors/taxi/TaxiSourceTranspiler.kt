package com.orbitalhq.schemaServer.core.adaptors.taxi

import com.orbitalhq.SourcePackage
import com.orbitalhq.functions.loaders.CustomFunctionSourceGenerator
import com.orbitalhq.schema.publisher.loaders.SourceGenerator
import com.orbitalhq.schemaServer.core.adaptors.avro.AvroTaxiSourceGenerator
import com.orbitalhq.schemaServer.core.adaptors.openapi.FileLoadingOpenApiSpecProvider
import com.orbitalhq.schemaServer.core.adaptors.openapi.OpenApiSourceGenerator
import com.orbitalhq.schemaServer.core.adaptors.protobuf.ProtobufTaxiSourceGenerator
import com.orbitalhq.schemaServer.core.adaptors.soap.SoapSchemaSourceGenerator
import com.orbitalhq.schemaServer.core.adaptors.xsd.FileLoadingXsdSourceConfigProvider
import com.orbitalhq.schemaServer.core.adaptors.xsd.XsdSchemaSourceGenerator
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
         ProtobufTaxiSourceGenerator(),
         XsdSchemaSourceGenerator(FileLoadingXsdSourceConfigProvider()),
         SoapSchemaSourceGenerator(FileLoadingXsdSourceConfigProvider()),
         CustomFunctionSourceGenerator.jarFileSourceGenerator(),
         CustomFunctionSourceGenerator.kotlinScriptSourceGenerator()
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
         val mergedHandlers = a.functionHandlers + b.functionHandlers
         a.copy(
            sources = mergedSources,
            additionalSources = mergedAdditionalSources,
            functionHandlers = mergedHandlers
         )
      }
   }
}
