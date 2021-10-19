package io.vyne.queryService.schemas

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import io.vyne.models.csv.ParsedCsvContent
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemaStore.SimpleSchemaProvider
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test

class FileToTypeParserServiceTest {
   val objectMapper =  jacksonObjectMapper()
   val testedObject = FileToTypeParserService(mock<SchemaProvider>(), objectMapper, mock {  })
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

   @Test
   fun canParseCsvWithAdditionalSchemaData() {
      val schema = TaxiSchema.from("""
         type Width inherits Int
         type Height inherits Int
         type Area inherits Int by Width * Height
      """.trimIndent())
      val service = FileToTypeParserService(
         SimpleSchemaProvider(schema),
         objectMapper,
         mock {  }
      )
      val parseResult = service.parseCsvToTypeWithAdditionalSchema(
         CsvWithSchemaParseRequest(
            csv = """width,height
               |10,5
            """.trimMargin(),
            schema = """
               model Rectangle {
                  width : Width by column("width")
                  height : Height by column("height")
                  area : Area
               }
            """.trimIndent()
         ),
         "Rectangle"
      )
      parseResult.types.should.have.size(1)
      parseResult.types.single().fullyQualifiedName.should.equal("Rectangle")

      parseResult.parsedTypedInstances.size.should.equal(1)
   }
}
