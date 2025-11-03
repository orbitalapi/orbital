package com.orbitalhq.cockpit.core.adaptors.soap

import com.orbitalhq.connectors.soap.SoapInvoker
import com.orbitalhq.firstRawObject
import com.orbitalhq.query.QueryContextEventBroker
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.schemaServer.core.adaptors.taxi.loadSourcePackage
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.testVyne
import io.kotest.common.runBlocking
import io.kotest.matchers.nulls.shouldNotBeNull
import org.junit.jupiter.api.Test

// These tests are for tests specifically where we need access to:
// - Schema Server Core (for loading projects from additional sources)
// - Soap invoker
// - Vyne query engine
// Most functionality should be tested closer to the declaration, rather than
// here in cockpit.
class SoapSchemaSourcesAdaptorTest {
   // This test specifically validates that when a SOAP service with xsd imports
   // was loaded using a mixed-sources approach
   // then the service can still be called.
   @Test
   fun `can invoke soap service using xsd imports`(): Unit = runBlocking {
      val source = loadSourcePackage("wsdl-with-imports")
      val schema = TaxiSchema.from(source)

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
}
