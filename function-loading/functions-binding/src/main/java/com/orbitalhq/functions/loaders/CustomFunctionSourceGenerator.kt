package com.orbitalhq.functions.loaders

import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.functions.TaxiFunctionProvider
import com.orbitalhq.functions.scanner.TaxiFunctionScanner
import com.orbitalhq.functions.taxigen.TaxiFunctionDefinitionGenerator
import com.orbitalhq.schema.publisher.loaders.SourceGenerator
import lang.taxi.packages.SourcesType

/**
 * General purpose generator that will
 * read the custom functions (provided by the FunctionClassProvider),
 * and generate the corresponding taxi, and the handler to invoke the function
 */
class CustomFunctionSourceGenerator(
   private val sourcesType: SourcesType,
   private val functionProvider: CustomFunctionProvider
) : SourceGenerator {

   companion object {
      const val FUNCTION_JAR_SOURCE_TYPE: SourcesType = "@orbital/function-jar"
      const val FUNCTION_KT_SCRIPT_SOURCE_TYPE: SourcesType = "@orbital/function-kts"

      // This is for tests
      private const val FUNCTION_INSTANCES_SOURCE_TYPE: SourcesType = "@orbital/function-instances"
      fun jarFileSourceGenerator() = CustomFunctionSourceGenerator(
         FUNCTION_JAR_SOURCE_TYPE,
         ServiceLoaderFunctionProvider()
      )
      fun kotlinScriptSourceGenerator() = CustomFunctionSourceGenerator(
         FUNCTION_KT_SCRIPT_SOURCE_TYPE,
         KotlinScriptFunctionProvider()
      )

      // For testing
      fun sourceGeneratorForFunctionProviderInstances(instances: List<TaxiFunctionProvider>) =
         CustomFunctionSourceGenerator(
            FUNCTION_INSTANCES_SOURCE_TYPE,
            SimpleFunctionClassProvider(instances)
         )

   }

   override fun supportsSources(sourcesType: SourcesType): Boolean {
      return sourcesType == this.sourcesType
   }

   override fun generateSourcePackage(
      sourceFiles: List<VersionedSource>,
      packageMetadata: PackageMetadata
   ): SourcePackage {
      val classes = functionProvider.loadFunctionClasses(sourceFiles, packageMetadata)
      val scanner = TaxiFunctionScanner()
      val handlers = scanner.scan(classes)
      val taxi = TaxiFunctionDefinitionGenerator.generate(handlers)

      return SourcePackage(
         packageMetadata,
         sources = listOf(
            VersionedSource.unversioned(
               "Generated from ${packageMetadata.identifier.id}  $sourcesType",
               taxi
            )
         ),
         functionHandlers = handlers
      )
   }
}
