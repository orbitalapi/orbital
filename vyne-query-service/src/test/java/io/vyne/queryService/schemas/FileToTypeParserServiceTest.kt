package io.vyne.queryService.schemas

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import io.vyne.models.csv.ParsedCsvContent
import io.vyne.schemaStore.SchemaProvider
import org.junit.Test

class FileToTypeParserServiceTest {
   val objectMapper =  jacksonObjectMapper()
   val testedObject = FileToTypeParserService(mock<SchemaProvider>(), objectMapper)
   val content = ParsedCsvContent(listOf("col1", "col2"), listOf(listOf("row1_col1", "row1_col2"), listOf("row2_col1", "row2_col2")))

   @Test
   fun canDownloadParsedContentsAsjson() {
      val jsonContentBytes = testedObject.downloadParsedData(content)
      val jsonString = String(jsonContentBytes)
      val expectedJson = """
         [{"col1":"row1_col1","col2":"row1_col2"},{"col1":"row2_col1","col2":"row2_col2"}]
      """.trimIndent()
      expectedJson.should.be.equal(jsonString)
   }
}
