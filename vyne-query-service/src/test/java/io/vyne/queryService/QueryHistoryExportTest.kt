package io.vyne.queryService

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.queryService.persistency.ReactiveDatabaseSupport
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class QueryHistoryExportTest {
   private lateinit var objectMapper: ObjectMapper

   private val personStr = """
      {
         "demo.Person": {
            "id": "1",
            "firstName": "Joe",
            "lastName": "Pass",
            "birthday": "19991212 09:12:13"
         }
      }
   """.trimIndent()

   private val personArrayStr = """
      {
         "lang.taxi.Array<demo.Person>": [
            {
                "id": "1",
                "firstName": "Joe",
                "lastName": "Pass",
                "birthday": "19991212 09:12:13"
            },
            {
                "id": "2",
                "firstName": "Herb",
                "lastName": "Ellis",
                "birthday": "19991212 20:23:24"
            }
         ]
      }
   """.trimIndent()

   @Before
   fun before() {
      val reactiveDatabaseSupport = ReactiveDatabaseSupport()
      reactiveDatabaseSupport.r2dbcCustomConversions()
      objectMapper = reactiveDatabaseSupport.objectMapper
   }

   @Test
   fun exportSingleToCsv() {
      val expected =
         """id,firstName,lastName,birthday
1,Joe,Pass,19991212 09:12:13
         """.trimIndent()

      val order = objectMapper.readValue(personStr, mutableMapOf<String, Any?>()::class.java)
      val historyExporter = QueryHistoryExporter(objectMapper)

      val actual = historyExporter.export(order, ExportType.CSV).toString(Charsets.UTF_8).trimIndent()

      assertEquals(expected, actual)
   }

   @Test
   fun exportArrayToCsv() {
      val expected =
         """id,firstName,lastName,birthday
1,Joe,Pass,19991212 09:12:13
2,Herb,Ellis,19991212 20:23:24
         """.trimIndent()

      val order = objectMapper.readValue(personArrayStr, mutableMapOf<String, Any?>()::class.java)
      val historyExporter = QueryHistoryExporter(objectMapper)

      val actual = historyExporter.export(order, ExportType.CSV).toString(Charsets.UTF_8).trimIndent()

      assertEquals(expected, actual)
   }

   @Test
   fun exportSingleToJson() {
      val expected = """{"id":"1","firstName":"Joe","lastName":"Pass","birthday":"19991212 09:12:13"}""".trimIndent()
      val order = objectMapper.readValue(personStr, mutableMapOf<String, Any?>()::class.java)
      val historyExporter = QueryHistoryExporter(objectMapper)
      val actual = historyExporter.export(order, ExportType.JSON).toString(Charsets.UTF_8)

      assertEquals(expected, actual)
   }

   @Test
   fun exportArrayToJson() {
      val expected = """[{"id":"1","firstName":"Joe","lastName":"Pass","birthday":"19991212 09:12:13"},{"id":"2","firstName":"Herb","lastName":"Ellis","birthday":"19991212 20:23:24"}]""".trimIndent()
      val order = objectMapper.readValue(personArrayStr, mutableMapOf<String, Any?>()::class.java)
      val historyExporter = QueryHistoryExporter(objectMapper)
      val actual = historyExporter.export(order, ExportType.JSON).toString(Charsets.UTF_8)

      assertEquals(expected, actual)
   }
}
