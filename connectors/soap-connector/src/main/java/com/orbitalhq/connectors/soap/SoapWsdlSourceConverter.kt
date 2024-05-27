package com.orbitalhq.connectors.soap

import com.orbitalhq.SourcePackage
import com.orbitalhq.asVersionedSource
import com.orbitalhq.schemas.readers.SourceConverterLoadResult
import com.orbitalhq.schemas.readers.SourceToTaxiConverter
import lang.taxi.TaxiDocument
import lang.taxi.generators.soap.SoapLanguage
import lang.taxi.generators.soap.TaxiGenerator
import mu.KotlinLogging

object SoapWsdlSourceConverter : SourceToTaxiConverter {
   private val logger = KotlinLogging.logger {}
   override fun canLoad(sourcePackage: SourcePackage): Boolean {
      return sourcePackage.languages.contains(SoapLanguage.WSDL)
   }

   override fun load(
      sourcePackage: SourcePackage,
      imports: List<TaxiDocument>
   ): SourceConverterLoadResult {
      val orignalWsdlSources = sourcePackage.sourcesWithPackageIdentifier
         .filter { it.language == SoapLanguage.WSDL }
      require(orignalWsdlSources.size == 1) { "Expected a single WSDL document, but found ${orignalWsdlSources.size}" }
      val versionedSource = orignalWsdlSources.single()
      val (taxiDoc,generatedTaxi) = try {
         TaxiGenerator().generateTaxiAndCompile(versionedSource.content)
      } catch (e: Exception) {
         logger.error(e) { "Failed to convert WSDL to taxi - ${e.message}" }
         throw e
      }
      val generatedTaxiSources = generatedTaxi.asVersionedSource(sourcePackage.packageMetadata.identifier, "GeneratedFromSoap")
      val transpiledPackage = SourcePackage.asTranspiledPackage(sourcePackage, generatedTaxiSources)
      return SourceConverterLoadResult(emptyList(),taxiDoc, listOf(transpiledPackage))
   }
}
