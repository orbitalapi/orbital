package com.orbitalhq.connectors.soap

import com.orbitalhq.SourcePackage
import com.orbitalhq.schemas.readers.SourceToTaxiConverter
import lang.taxi.CompilationError
import lang.taxi.TaxiDocument
import lang.taxi.generators.soap.SoapLanguage
import lang.taxi.generators.soap.TaxiGenerator
import mu.KotlinLogging

object SoapWsdlSourceConverter : SourceToTaxiConverter {
   private val logger = KotlinLogging.logger {}
   override fun canLoad(sourcePackage: SourcePackage): Boolean {
      return sourcePackage.languages.contains(SoapLanguage.WSDL)
   }

   override fun loadAll(
      sourcePackages: List<SourcePackage>,
      imports: List<TaxiDocument>
   ): Pair<List<CompilationError>, TaxiDocument> {
      val allErrors = mutableListOf<CompilationError>()
      val merged = sourcePackages.fold(TaxiDocument.empty()) { acc, sourcePackage ->
         val (errors, taxi) = load(sourcePackage, imports)
         allErrors.addAll(errors)
         acc.merge(taxi)
      }
      return allErrors to merged
   }

   fun load(
      sourcePackage: SourcePackage,
      imports: List<TaxiDocument>
   ): Pair<List<CompilationError>, TaxiDocument> {
      val sources = sourcePackage.sourcesWithPackageIdentifier
         .filter { it.language == SoapLanguage.WSDL }
      require(sources.size == 1) { "Expected a single WSDL document, but found ${sources.size}" }
      val versionedSource = sources.single()
      val taxiDoc = try {
         TaxiGenerator().generateTaxiDocument(versionedSource.content)
      } catch (e: Exception) {
         logger.error(e) { "Failed to convert WSDL to taxi - ${e.message}" }
         throw e
      }

      return emptyList<CompilationError>() to taxiDoc
   }
}
