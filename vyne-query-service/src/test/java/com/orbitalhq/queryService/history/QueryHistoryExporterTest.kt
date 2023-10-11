package com.orbitalhq.queryService.history

import app.cash.turbine.test
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.doThrow
import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import com.orbitalhq.history.db.QueryHistoryRecordRepository
import com.orbitalhq.history.db.QueryResultRowRepository
import com.orbitalhq.history.rest.export.ExportFormat
import com.orbitalhq.history.rest.export.QueryHistoryExporter
import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.csv.CsvFormatSpec
import com.orbitalhq.models.json.Jackson
import com.orbitalhq.query.ProjectionAnonymousTypeProvider
import com.orbitalhq.query.QueryResponse
import com.orbitalhq.query.history.QueryResultRow
import com.orbitalhq.query.history.QuerySummary
import com.orbitalhq.queryService.BaseQueryServiceTest
import com.orbitalhq.schema.spring.SimpleTaxiSchemaProvider
import com.orbitalhq.spring.http.VyneQueryServiceExceptionProvider
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import lang.taxi.Compiler
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.dao.EmptyResultDataAccessException
import java.time.Instant
import java.util.*
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
      queryExporter = QueryHistoryExporter(objectMapper, resultRowRepository, historyRecordRepository, schemaProvider, VyneQueryServiceExceptionProvider(), listOf(CsvFormatSpec))


      queryExporter.export("fakeId", ExportFormat.CSV)
         .test {
            val error = awaitError()
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
            awaitItem().trim().should.equal("firstName,lastName,age")
            awaitItem().trim().should.equal("Jimmy,Schmitts,50")
            awaitItem().trim().should.equal("Peter,Papps,50")
            awaitComplete()
         }
   }

   @Test
   fun `when exporting csv with null values then data is in correct columns`()  = runBlocking {
      prepareQueryHistoryResults("""[
         |{ "firstName" : "Jimmy" , "lastName" : null , "age" : 50 },
         |{ "firstName" : null , "lastName" : null , "age" : 50 } ]""".trimMargin())
      queryExporter.export(queryId = "123", exportFormat = ExportFormat.CSV)
         .test {
            awaitItem().trim().should.equal("firstName,lastName,age")
            awaitItem().trim().should.equal("Jimmy,,50")
            awaitItem().trim().should.equal(",,50")
            awaitComplete()
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

   @Test
   fun canExportValuesAsModelFormatSpec() = runBlocking {
      prepareQueryHistoryResults("""[
         |{ "firstName" : "Jimmy" , "lastName" : "Schmitts" , "age" : 50 },
         |{ "firstName" : "Peter" , "lastName" : "Papps" , "age" : 50 } ]""".trimMargin())
      queryExporter.export(queryId = "123", exportFormat = ExportFormat.CUSTOM)
         .test {
            awaitItem().trim().should.equal(
               "firstName|lastName|age\r\n" +
               "Jimmy|Schmitts|50")
            awaitItem().trim().should.equal("Peter|Papps|50")
            awaitComplete()
         }
   }

   @Test
   @Ignore("this needs investigation - can't tell if the test is wrong, or if the export is broken")
   fun canExportAnonymousValuesAsModelFormatSpec() = runBlocking {
      val (schemaProvider, schema) = SimpleTaxiSchemaProvider.from(
         """
         type FirstName inherits String
         type LastName inherits String
         type Age inherits Int
         model Person {
            firstName : FirstName
            lastName : LastName
            age : Age
         }
      """
      )
      val vyneQlQuery = """
         find {
            Person[]
         } as
         @com.orbitalhq.formats.Csv(
                     delimiter = "|",
                     nullValue = "NULL"
                  )
         {
              firstName: FirstName
              age: Age
         }[]
      """.trimIndent()
      val taxiQl = Compiler(source = vyneQlQuery, importSources = listOf(schema.taxi)).queries().first()
      val vyneAnonymousType =  ProjectionAnonymousTypeProvider.toVyneAnonymousType(
         taxiQl.projectedType!!,
         schema
      )

      val results = """[
         |{ "firstName" : "Jimmy" , "age" : 50 },
         |{ "firstName" : "Peter" , "age" : 50 } ]"""
         .trimMargin()
      val typedCollection = TypedInstance.from(vyneAnonymousType, results, schema = schema, source = Provided) as TypedCollection
      this@QueryHistoryExporterTest.schemaProvider = schemaProvider
      prepareQueryHistoryResults(
         typedCollection,
         QuerySummary(
            queryId = "123",
            clientQueryId =  UUID.randomUUID().toString(),
            taxiQl = vyneQlQuery,
            queryJson = null,
            startTime = Instant.now(),
            responseStatus = QueryResponse.ResponseStatus.COMPLETED,
            anonymousTypesJson = objectMapper.writeValueAsString(setOf(vyneAnonymousType.collectionType))
         )
      )
      queryExporter.export(queryId = "123", exportFormat = ExportFormat.CUSTOM)
         .test {
            awaitItem().trim().should.equal(
               "firstName|age\r\n" +
                  "Jimmy|50")
            awaitItem().trim().should.equal("Peter|50")
            awaitComplete()
         }
   }


   private fun prepareQueryHistoryResults(results: String) {
      val (schemaProvider, schema) = SimpleTaxiSchemaProvider.from(
         """
         @com.orbitalhq.formats.Csv(
            delimiter = "|",
            nullValue = "NULL",
            useFieldNamesAsColumnNames = true
         )
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
      prepareQueryHistoryResults(typedInstance)
   }

   private fun prepareQueryHistoryResults(typedInstance: TypedCollection, querySummary: QuerySummary = mock()) {
      val queryResults: List<QueryResultRow> = typedInstance.map { it.toTypeNamedInstance() }
         .map { QueryResultRow(queryId = "", json = objectMapper.writeValueAsString(it), valueHash = 123) }

      resultRowRepository = mock {
         on { findAllByQueryId(any()) } doReturn queryResults
      }
      historyRecordRepository = mock {
         on { findByQueryId(any()) } doReturn querySummary
      }
      queryExporter = QueryHistoryExporter(objectMapper, resultRowRepository, historyRecordRepository, schemaProvider, VyneQueryServiceExceptionProvider(), listOf(CsvFormatSpec))
   }
}
