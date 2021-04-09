//package io.vyne.queryService
//
//import com.fasterxml.jackson.databind.ObjectMapper
//import io.vyne.models.TypeNamedInstance
//import io.vyne.queryService.history.ExportType
//import io.vyne.queryService.history.QueryHistoryExporter
////import io.vyne.queryService.history.db.ReactiveDatabaseSupport
//import io.vyne.spring.SimpleTaxiSchemaProvider
//import org.junit.Before
//import org.junit.Test
//import kotlin.test.assertEquals
//
//class QueryHistoryExportTest {
//   private lateinit var objectMapper: ObjectMapper
//   private val schemaProvider = SimpleTaxiSchemaProvider("""
//namespace demo
//model Person {
//         id : PersonId as String
//         firstName : FirstName as String
//         lastName : LastName as String
//         birthday : String
//         address : String?
//         alwaysNull : String?
//      }
//      """)
//
//   private val personStr = """
//      {
//         "demo.Person": {
//            "id": "1",
//            "firstName": "Joe",
//            "lastName": "Pass",
//            "birthday": "19991212 09:12:13"
//         }
//      }
//   """.trimIndent()
//
//   private val personArrayStr = """
//      {
//         "lang.taxi.Array<demo.Person>": [
//            {
//                "id": "1",
//                "firstName": "Joe",
//                "lastName": "Pass",
//                "birthday": "19991212 09:12:13"
//            },
//            {
//                "id": "2",
//                "firstName": "Herb",
//                "lastName": "Ellis",
//                "birthday": "19991212 20:23:24"
//            }
//         ]
//      }
//   """.trimIndent()
//
//   @Before
//   fun before() {
//      val reactiveDatabaseSupport = ReactiveDatabaseSupport()
//      reactiveDatabaseSupport.r2dbcCustomConversions()
//      objectMapper = reactiveDatabaseSupport.objectMapper
//   }
//
//   @Test
//   fun exportSingleToCsv() {
//      val expected =
//         """id,firstName,lastName,birthday
//1,Joe,Pass,19991212 09:12:13
//         """.trimIndent()
//
//      val order = objectMapper.readValue(personStr, mutableMapOf<String, Any?>()::class.java)
//      val historyExporter = QueryHistoryExporter(objectMapper, schemaProvider)
//
//      val actual = historyExporter.export(order, ExportType.CSV).toString(Charsets.UTF_8).trimIndent()
//
//      assertEquals(expected, actual)
//   }
//
//   @Test
//   fun exportArrayToCsv() {
//      val expected = """id,firstName,lastName,birthday,address,alwaysNull
//1,Joe,Pass,19991212 09:12:13,,
//2,Herb,Ellis,19991212 20:23:24,,"""
//
//      val order = objectMapper.readValue(personArrayStr, mutableMapOf<String, Any?>()::class.java)
//      val historyExporter = QueryHistoryExporter(objectMapper, schemaProvider)
//
//      val actual = historyExporter.export(order, ExportType.CSV).toString(Charsets.UTF_8).trimIndent()
//
//      assertEquals(expected, actual)
//   }
//
//   @Test
//   fun exportListTypeNamedInstanceToCsv() {
//      val expected =
//         """id,firstName,lastName,birthday,address,alwaysNull
//1,Joe,Pass,19991212 09:12:13,,
//2,Herb,Ellis,19991212 20:23:24,Downing Street,
//         """.trimIndent()
//
//      val order = mapOf(
//         "lang.taxi.Array<demo.Person>" to listOf(
//            TypeNamedInstance("demo.Person",
//               mapOf("id" to TypeNamedInstance("id", 1),
//                  "firstName" to TypeNamedInstance("firstName", "Joe"),
//                  "lastName" to TypeNamedInstance("lastName", "Pass"),
//                  "birthday" to TypeNamedInstance("birthday", "19991212 09:12:13"))),
//            TypeNamedInstance("demo.Person",
//               mapOf("id" to TypeNamedInstance("age", 2),
//                  "firstName" to TypeNamedInstance("firstName", "Herb"),
//                  "lastName" to TypeNamedInstance("lastName", "Ellis"),
//                  "birthday" to TypeNamedInstance("birthday", "19991212 20:23:24"),
//                  "address" to TypeNamedInstance("address", "Downing Street")))
//         )
//      )
//
//      val historyExporter = QueryHistoryExporter(objectMapper, schemaProvider)
//
//      val actual = historyExporter.export(order, ExportType.CSV).toString(Charsets.UTF_8).trimIndent()
//
//      assertEquals(expected, actual)
//   }
//
//   @Test
//   fun exportSingleToJson() {
//      val expected = """{"id":"1","firstName":"Joe","lastName":"Pass","birthday":"19991212 09:12:13"}""".trimIndent()
//      val order = objectMapper.readValue(personStr, mutableMapOf<String, Any?>()::class.java)
//      val historyExporter = QueryHistoryExporter(objectMapper, schemaProvider)
//      val actual = historyExporter.export(order, ExportType.JSON).toString(Charsets.UTF_8)
//
//      assertEquals(expected, actual)
//   }
//
//   @Test
//   fun exportArrayToJson() {
//      val expected = """[{"id":"1","firstName":"Joe","lastName":"Pass","birthday":"19991212 09:12:13"},{"id":"2","firstName":"Herb","lastName":"Ellis","birthday":"19991212 20:23:24"}]""".trimIndent()
//      val order = objectMapper.readValue(personArrayStr, mutableMapOf<String, Any?>()::class.java)
//      val historyExporter = QueryHistoryExporter(objectMapper, schemaProvider)
//      val actual = historyExporter.export(order, ExportType.JSON).toString(Charsets.UTF_8)
//
//      assertEquals(expected, actual)
//   }
//}
