package com.orbitalhq.schemas.readers

import com.orbitalhq.SourcePackage
import lang.taxi.CompilationError
import lang.taxi.Compiler
import lang.taxi.TaxiDocument
import lang.taxi.sources.SourceCodeLanguages
import org.antlr.v4.runtime.CharStreams

object TaxiSourceConverter : SourceToTaxiConverter {
   override fun canLoad(sourcePackage: SourcePackage): Boolean {
      return sourcePackage.languages.contains(SourceCodeLanguages.TAXI)
         // Also consider where we don't have any sources.
         // This lets us
         || sourcePackage.languages.isEmpty()
   }

   override fun loadAll(
      sourcePackages: List<SourcePackage>,
      imports: List<TaxiDocument>
   ): SourceConverterLoadResult {
      val sourceStreams = sourcePackages.flatMap { it.sourcesWithPackageIdentifier }
         .filter { it.language == SourceCodeLanguages.TAXI }
         .map {  CharStreams.fromString(it.content, it.packageQualifiedName)  }

      val (errors,taxiDoc) = Compiler(sourceStreams, imports).compileWithMessages()
      return SourceConverterLoadResult(errors, taxiDoc, sourcePackages)
   }

   override fun load(sourcePackage: SourcePackage, imports: List<TaxiDocument>): SourceConverterLoadResult {
      error("Not implemented - call loadAll() for taxi sources")
   }
//   override fun load(
//      sourcePackage: SourcePackage,
//      imports: List<TaxiDocument>
//   ): Pair<List<CompilationError>, TaxiDocument> {
//      return Compiler(
//         sourcePackage.sourcesWithPackageIdentifier
//            .filter { it.language == SourceCodeLanguages.TAXI }
//            .map { CharStreams.fromString(it.content, it.packageQualifiedName) },
//         imports
//      ).compileWithMessages()
//   }
}
