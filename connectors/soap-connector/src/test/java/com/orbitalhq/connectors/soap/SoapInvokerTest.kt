package com.orbitalhq.connectors.soap

import com.google.common.io.Resources
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
import lang.taxi.generators.soap.TaxiGenerator
import org.junit.Ignore
import org.junit.Test

@Ignore // Upgrading xSD parsing
class SoapInvokerTest {

   @Test
   fun `emits tracing events when invoking SOAP service and includes request and response payload`(): Unit = runBlocking {
      val generator = TaxiGenerator()
      val wsdlUrl = Resources.getResource("TrimmedCountryInfoServiceSpec.wsdl")
      val taxiDoc = generator.generateTaxiDocument(wsdlUrl)
      val schema = TaxiSchema(taxiDoc, emptyList())

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
   fun `can invoke soap service`(): Unit = runBlocking {
      val generator = TaxiGenerator()
      val wsdlUrl = Resources.getResource("TrimmedCountryInfoServiceSpec.wsdl")
      val taxiDoc = generator.generateTaxiDocument(wsdlUrl)
      val schema = TaxiSchema(taxiDoc, emptyList())

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
}
