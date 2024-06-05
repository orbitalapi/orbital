package com.orbitalhq.schemas.readers

import com.orbitalhq.SourcePackage
import lang.taxi.CompilationError
import lang.taxi.TaxiDocument
import lang.taxi.generators.GeneratedTaxiCode

/**
 * Converter which accepts source code and returns an actual taxi document.
 *
 * This is a more sophisticated approach then using a simple SchemaSourcesAdaptor.
 * Use this approach when you need to attach special adaption / behaviour to actual generated
 * services.
 *
 * For example - services generated from SOAP need to be able to fetch the original WSDL in order
 * to execute the service. A SourceToTaxiConverter allows embedding the original wsdl in the created
 * Service, which can be used at execution time.
 *
 * If you don't need to customize this behaviour, it's preferred to not use a SourceToTaxiConverter,
 * and do the work in a SchemaSourcesAdaptor, emitting taxi code.
 *
 * DEPRECATED
 * This approach turned out to be good in theory, but hit issues because the attached
 * sources are lost when we do a round trip from Schema -> Taxi -> Schema (eg.,
 * when editing a package).
 *
 * Instead, use a SchemaSourcesAdaptor that emits a SourceMap.
 * See AvroSchemaSourcesAdaptor for an example of how to produce a source map,
 * and AvroFormatDeserializer / AvroSchemaCollection for an example of how to use the sourcemap at runtime
 */
@Deprecated("Use a SchemaSourcesAdaptor with a SourceMap instead")
interface SourceToTaxiConverter {
   fun canLoad(sourcePackage: SourcePackage): Boolean

   // Calling load() with a single package causes problems until we can get dependency resolution between
   // packages working.
   // Iterating over Taxi packages and compiling them one-by-one means that if something is referenced in package-0,
   // but not declared until package-1, we don't resolve it properly.
   // Therefore, for now we're working around this by grouping all the sources
   // of a language together and loading them, (starting with Taxi first).
   // That doesn't solve the problem, but works around them for now.
   // This means that things like Soap loaders are loaded after the taxi projects,
   // so for now, by convention, their dependencies are already loaded.
   fun load(sourcePackage: SourcePackage, imports: List<TaxiDocument>): SourceConverterLoadResult

   fun loadAll(sourcePackages: List<SourcePackage>, imports: List<TaxiDocument>): SourceConverterLoadResult {
      val allErrors = mutableListOf<CompilationError>()
      val allTranspiledSources = mutableListOf<SourcePackage>()
      val merged = sourcePackages.fold(TaxiDocument.empty()) { acc, sourcePackage ->
         val (errors: List<CompilationError>, taxi: TaxiDocument, transpiledSource: List<SourcePackage>) = load(sourcePackage, imports)
         allErrors.addAll(errors)
         allTranspiledSources.addAll(transpiledSource)
         acc.merge(taxi)
      }
      return SourceConverterLoadResult(allErrors, merged, allTranspiledSources)
   }
}


/**
 * Returns from a SourceToTaxiConverter.
 * Allows the converter to additionally return the transpiled source.
 * (eg., if the converter received a SourcePackage containing OpenAPI, and transpiled OpenAPI -> Taxi,
 * can return the transpiled Taxi)
 */
data class SourceConverterLoadResult(
   val errors: List<CompilationError>,
   val taxi: TaxiDocument,
   val transpiledSource: List<SourcePackage>
) {
   companion object {
      fun empty() = SourceConverterLoadResult(emptyList(), TaxiDocument.empty(), emptyList())
   }
}
