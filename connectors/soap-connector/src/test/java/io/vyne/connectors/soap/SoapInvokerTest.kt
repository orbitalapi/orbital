package io.vyne.connectors.soap

import com.google.common.io.Resources
import io.kotest.common.runBlocking
import io.kotest.matchers.nulls.shouldNotBeNull
import io.vyne.firstRawObject
import io.vyne.schema.api.SimpleSchemaProvider
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.testVyne
import lang.taxi.generators.soap.TaxiGenerator
import org.junit.Test

class SoapInvokerTest {

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
