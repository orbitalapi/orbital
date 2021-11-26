package io.vyne.query

import com.google.common.io.Resources
import com.winterbe.expekt.should
import io.vyne.models.TypedInstance
import io.vyne.rawObjects
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.testVyne
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import java.nio.file.Paths

class BugInvestigationTest {

   @Test
   fun `whats going on`():Unit = runBlocking {
      val taxonomyPath = Paths.get(Resources.getResource("ultumus/taxonomy").toURI())
      val schema = TaxiSchema.forPackageAtPath(taxonomyPath)
      val (vyne,stub) = testVyne(schema)
      val response = File(Resources.getResource("ultumus/samples/IV0000A1TLJ.json").toURI()).readText()
      val indexComposition = TypedInstance.from(schema.type("IndexCompositionSummary"), response, schema)
      stub.addResponse("getClients", indexComposition)

      val results = vyne.query("""
         given { id: IndexId = "IV0000A1TLJ"}
         findOne { IndexCompositionSummary } as {
             BENCHMARK_ID: ProviderCode
         }
      """.trimIndent())
         .rawObjects()
      results.should.not.be.empty

   }
}
