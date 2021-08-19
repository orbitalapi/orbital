package io.vyne.queryService.history

import app.cash.turbine.test
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.doThrow
import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import io.vyne.history.export.ExportFormat
import io.vyne.history.export.QueryHistoryExporter
import io.vyne.history.db.QueryHistoryRecordRepository
import io.vyne.history.db.QueryResultRowRepository
import io.vyne.models.Provided
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.json.Jackson
import io.vyne.query.history.QueryResultRow
import io.vyne.query.history.QuerySummary
import io.vyne.queryService.BaseQueryServiceTest
import io.vyne.spring.SimpleTaxiSchemaProvider
import io.vyne.spring.http.VyneQueryServiceExceptionProvider
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.dao.EmptyResultDataAccessException
import kotlin.time.ExperimentalTime

@ExperimentalTime
@FlowPreview
class QueryHistoryExporterTest : BaseQueryServiceTest() {

   lateinit var queryExporter: QueryHistoryExporter
   lateinit var resultRowRepository: QueryResultRowRepository
   lateinit var historyRecordRepository: QueryHistoryRecordRepository
   val objectMapper = Jackson.defaultObjectMapper
   lateinit var schemaProvider: SimpleTaxiSchemaProvider

   @Before
   fun setup() {

   }

   @Test
   fun shouldThrowExceptionIfQueryingWithUnknownQueryId() = runBlocking {
      prepareQueryHistoryResults("""[
         |{ "firstName" : "Jimmy" , "lastName" : "Schmitts" , "age" : 50 },
         |{ "firstName" : "Peter" , "lastName" : "Papps" , "age" : 50 } ]""".trimMargin())
      historyRecordRepository = mock {
         on { findByQueryId(any()) } doThrow EmptyResultDataAccessException(1)
      }
      queryExporter = QueryHistoryExporter(objectMapper, resultRowRepository, historyRecordRepository, schemaProvider, VyneQueryServiceExceptionProvider())


      queryExporter.export("fakeId", ExportFormat.CSV)
         .test {
            val error = expectError()
            error.message.should.equal("No query with id fakeId was found")
         }

   }

   @Test
   fun canExportValuesAsCsv() = runBlocking {
      prepareQueryHistoryResults("""[
         |{ "firstName" : "Jimmy" , "lastName" : "Schmitts" , "age" : 50 },
         |{ "firstName" : "Peter" , "lastName" : "Papps" , "age" : 50 } ]""".trimMargin())
      queryExporter.export(queryId = "123", exportFormat = ExportFormat.CSV)
         .test {
            expectItem().trim().should.equal("firstName,lastName,age")
            expectItem().trim().should.equal("Jimmy,Schmitts,50")
            expectItem().trim().should.equal("Peter,Papps,50")
            expectComplete()
         }
   }

   @Test
   fun `when exporting csv with null values then data is in correct columns`()  = runBlocking {
      prepareQueryHistoryResults("""[
         |{ "firstName" : "Jimmy" , "lastName" : null , "age" : 50 },
         |{ "firstName" : null , "lastName" : null , "age" : 50 } ]""".trimMargin())
      queryExporter.export(queryId = "123", exportFormat = ExportFormat.CSV)
         .test {
            expectItem().trim().should.equal("firstName,lastName,age")
            expectItem().trim().should.equal("Jimmy,,50")
            expectItem().trim().should.equal(",,50")
            expectComplete()
         }
   }

   @Test
   fun canExportValuesAsJson() = runBlocking {
      prepareQueryHistoryResults("""[
         |{ "firstName" : "Jimmy" , "lastName" : "Schmitts" , "age" : 50 },
         |{ "firstName" : "Peter" , "lastName" : "Papps" , "age" : 50 } ]""".trimMargin())
      val json = queryExporter.export(queryId = "123", exportFormat = ExportFormat.JSON)
         .toList().joinToString(separator = "")
      json.should.not.be.empty
      JSONAssert.assertEquals(
         """[
         |{ "firstName" : "Jimmy" , "lastName" : "Schmitts" , "age" : 50 },
         |{ "firstName" : "Peter" , "lastName" : "Papps" , "age" : 50 } ]
      """.trimMargin(), json, true
      )
   }

   private fun prepareQueryHistoryResults(results: String) {
      val (schemaProvider, schema) = SimpleTaxiSchemaProvider.from(
         """
         model Person {
            firstName : String
            lastName : String
            age : Int
         }
      """
      )
      this.schemaProvider = schemaProvider

      val typedInstance = TypedInstance.from(
         schema.type("Person[]"), results, schema = schema, source = Provided
      ) as TypedCollection
      val queryResults: List<QueryResultRow> = typedInstance.map { it.toTypeNamedInstance() }
         .map { QueryResultRow(queryId = "", json = objectMapper.writeValueAsString(it), valueHash = 123) }

      resultRowRepository = mock {
         on { findAllByQueryId(any()) } doReturn queryResults
      }
      val queryHistoryRecord: QuerySummary = mock()
      historyRecordRepository = mock {
         on { findByQueryId(any()) } doReturn queryHistoryRecord
      }
      queryExporter = QueryHistoryExporter(objectMapper, resultRowRepository, historyRecordRepository, schemaProvider, VyneQueryServiceExceptionProvider())
   }
}
