package io.vyne.schemas.readers

import io.vyne.SourcePackage
import lang.taxi.CompilationError
import lang.taxi.TaxiDocument

/**
 * Design experiment:
 * We currently use special source adaptors to convert non-taxi sources (eg., OpenAPI)
 * to Taxi, and then load the taxi.
 *
 * That gets complex when sources need to also provide serialization details, like
 * SOAP and Protobuf clients.
 *
 * Instead, playing with an approach of leave the source as-is, and convert to Taxi
 * when the TaxiSchema object is parsing the sources.
 */
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
//   fun load(sourcePackage: SourcePackage, imports: List<TaxiDocument>): Pair<List<CompilationError>, TaxiDocument>

   fun loadAll(sourcePackages: List<SourcePackage>, imports: List<TaxiDocument>): Pair<List<CompilationError>, TaxiDocument>
}


