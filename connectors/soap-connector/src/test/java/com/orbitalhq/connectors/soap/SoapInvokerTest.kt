package com.orbitalhq.connectors.soap

import com.google.common.io.Resources
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.asVersionedSource
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import com.orbitalhq.firstRawObject
import com.orbitalhq.query.QueryContextEventBroker
import com.orbitalhq.query.tracing.HttpRequest
import com.orbitalhq.query.tracing.HttpResponse
import com.orbitalhq.query.tracing.SpanState
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.testVyne
import com.orbitalhq.utils.Ids
import io.kotest.matchers.maps.shouldContainKeys
import lang.taxi.generators.GeneratedTaxiCode
import lang.taxi.generators.soap.TaxiGenerator
import lang.taxi.sources.SourceCode
import org.junit.Test

class SoapInvokerTest {

   @Test
   fun `emits tracing events when invoking SOAP service and includes request and response payload`(): Unit =
      runBlocking {
         val generator = TaxiGenerator()
         val wsdlUrl = Resources.getResource("TrimmedCountryInfoServiceSpec.wsdl")
         val generatedCodeAndSources = generator.wsdlToGeneratedSources(wsdlUrl.toURI())
         val schema = sourcesToTaxiSchema(generatedCodeAndSources)

         val vyne = testVyne(
            schema, listOf(
               SoapInvoker(
                  SimpleSchemaProvider(schema)
               )
            )
         )

         val (eventBroker, eventSink) = QueryContextEventBroker.withTestTraceSpan()

         val result = vyne.query(
            """
         given { countryCode:  com.test.IsoCountryCode = "NZ" }
         find { tCountryInfo }
      """.trimIndent(),
            eventBroker = eventBroker
         ).firstRawObject()

         // Should have request and response events
         eventSink.collectedEvents.shouldHaveSize(2)

         // Verify request event
         val requestEvent = eventSink.collectedEvents.first()
         requestEvent.spanState.shouldBe(SpanState.ACTIVE)
         val requestMetadata = requestEvent.exchangeMetadata.shouldBeInstanceOf<HttpRequest>()
         requestEvent.eventVerb.shouldBe("POST") // SOAP typically uses POST
         val requestPayload = requestMetadata.payload()
         requestPayload.shouldNotBeNull()
         requestPayload.shouldContain("NZ") // Should contain the country code parameter

         // Verify response event
         val responseEvent = eventSink.collectedEvents.last()
         responseEvent.spanState.shouldBe(SpanState.COMPLETE)
         val responseMetadata = responseEvent.exchangeMetadata.shouldBeInstanceOf<HttpResponse>()
         responseEvent.eventVerb.shouldBe("Invoke response")
         responseMetadata.responseCode.shouldBe(200)
         val responsePayload = responseMetadata.payload()
         responsePayload.shouldNotBeNull()

         // Verify actual query result
         result.shouldNotBeNull()
      }

   @Test
   fun `can invoke soap service using xsd imports`(): Unit = runBlocking {
      val generator = TaxiGenerator()
      val wsdlUrl = Resources.getResource("wsdl-with-imports/CountryInfoService.wsdl")
      val generatedCodeAndSources = generator.wsdlToGeneratedSources(wsdlUrl.toURI())
      val schema = sourcesToTaxiSchema(generatedCodeAndSources)

      val vyne = testVyne(
         schema, listOf(
            SoapInvoker(
               SimpleSchemaProvider(schema)
            )
         )
      )
      val (eventBroker, eventSink) = QueryContextEventBroker.withTestTraceSpan()

      val result = try {
         vyne.query(
            """
         given { countryCode:  com.test.IsoCountryCode = "NZ" }
         find { tCountryInfo }
      """.trimIndent(),
            eventBroker = eventBroker
         )
            .firstRawObject()
      } catch (e: Exception) {
         null
      }
      result.shouldNotBeNull()
   }

   @Test
   fun `can invoke soap service`(): Unit = runBlocking {
      val generator = TaxiGenerator()
      val wsdlUrl = Resources.getResource("TrimmedCountryInfoServiceSpec.wsdl")
      val generatedCodeAndSources = generator.wsdlToGeneratedSources(wsdlUrl.toURI())
      val schema = sourcesToTaxiSchema(generatedCodeAndSources)

      val vyne = testVyne(
         schema, listOf(
            SoapInvoker(
               SimpleSchemaProvider(schema)
            )
         )
      )
      val result = vyne.query(
         """
         given { countryCode:  com.test.IsoCountryCode = "NZ" }
         find { tCountryInfo }
      """.trimIndent()
      )
         .firstRawObject()
      result.shouldNotBeNull()
   }

   @Test
   fun `can directly invoke soap service`(): Unit = runBlocking {
      val generator = TaxiGenerator()
      val wsdlUrl = Resources.getResource("TrimmedCountryInfoServiceSpec.wsdl")
      val generatedCodeAndSources = generator.wsdlToGeneratedSources(wsdlUrl.toURI())
      val schema = sourcesToTaxiSchema(generatedCodeAndSources)

      val vyne = testVyne(
         schema, listOf(
            SoapInvoker(
               SimpleSchemaProvider(schema)
            )
         )
      )
      val result = vyne.query(
         """
         find { CountryInfo: tCountryInfo =  CountryInfoService::FullCountryInfo(
            { sCountryISOCode: "NZ" }
         ) }
      """.trimIndent()
      )
         .firstRawObject()
      result.shouldNotBeNull()
      result.shouldContainKeys("CountryInfo")

   }
}


fun sourcesToTaxiSchema(
   sources: Pair<GeneratedTaxiCode, List<SourceCode>>,
   identifier: PackageIdentifier = PackageIdentifier.fromId("com.orbital/test/0.1.0")
): TaxiSchema {
   val (generated, sourceCode) = sources
   val versionedSources = sourceCode.map { it.asVersionedSource() }
   val sourcePackage = SourcePackage.asTranspiledPackage(
      packageMetadata = PackageMetadata.from(identifier),
      originalSources = versionedSources,
      generatedTaxiSources = generated.asVersionedSource(identifier, Ids.id("Test", 4)),
      sourceMap = generated.sourceMap
   )
   return TaxiSchema.from(sourcePackage)
}
