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
   }

   override fun loadAll(
      sourcePackages: List<SourcePackage>,
      imports: List<TaxiDocument>
   ): Pair<List<CompilationError>, TaxiDocument> {
      val sourceStreams = sourcePackages.flatMap { it.sourcesWithPackageIdentifier }
         .filter { it.language == SourceCodeLanguages.TAXI }
         .map {  CharStreams.fromString(it.content, it.packageQualifiedName)  }
      return Compiler(sourceStreams, imports).compileWithMessages()
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
