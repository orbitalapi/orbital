package io.vyne.queryService

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
import io.vyne.query.HistoryQueryResponse
import io.vyne.query.RemoteCall
import io.vyne.query.ResultMode
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.QualifiedName
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import reactor.core.publisher.Mono
import java.time.Instant

class QueryHistoryServiceTest {
   @Test
   fun `can download remote calls for a given query`() {
      val mockQueryHistory = mock<QueryHistory>()
      val mockSchemaProvider = mock<SchemaProvider>()
      val queryHistoryExporter = QueryHistoryExporter(jacksonObjectMapper(), mockSchemaProvider)
      val queryHistoryService = QueryHistoryService(mockQueryHistory, queryHistoryExporter)
      val queryId = "1"

      val sampleRemoteCall = RemoteCall(service = QualifiedName("test.sample.Service"),
         operation = "operation1",
         addresss = "http://localhost",
         durationMs = 1,
         method = "GET",
         requestBody = null,
         response = null,
         responseTypeName = QualifiedName("void"),
         resultCode = 200)

      val historyQueryResponse = HistoryQueryResponse(results = mapOf(),
         error = null,
         fullyResolved = true,
         profilerOperation = null,
         queryResponseId = queryId,
         resultMode = ResultMode.VERBOSE,
         timings = mapOf(),
         unmatchedNodes = listOf(),
         remoteCalls = listOf(sampleRemoteCall)
      )
      whenever(mockQueryHistory.get(anyString()))
         .thenReturn(Mono.just(VyneQlQueryHistoryRecord("query", historyQueryResponse, Instant.now())))
      val jsonFileContent = queryHistoryService.getRemoteCallExport(queryId)
      val fileContent = jsonFileContent.block()
      val jsonContent = String(fileContent)
      jsonContent.should.be.equal("""
         [{"service":"test.sample.Service","addresss":"http://localhost","operation":"operation1","responseTypeName":"void","method":"GET","requestBody":null,"resultCode":200,"durationMs":1,"response":null,"operationQualifiedName":"test.sample.Service@@operation1"}]
      """.trimIndent())
   }
}
