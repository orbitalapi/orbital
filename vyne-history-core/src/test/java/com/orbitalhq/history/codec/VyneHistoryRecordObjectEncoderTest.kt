package com.orbitalhq.history.codec

import com.winterbe.expekt.should
import io.netty.buffer.PooledByteBufAllocator
import com.orbitalhq.query.HttpExchange
import com.orbitalhq.query.QueryResponse
import com.orbitalhq.query.ResponseMessageType
import com.orbitalhq.query.history.FlowChartData
import com.orbitalhq.query.history.LineageRecord
import com.orbitalhq.query.history.QueryResultRow
import com.orbitalhq.query.history.QuerySankeyChartRow
import com.orbitalhq.query.history.QuerySummary
import com.orbitalhq.query.history.RemoteCallResponse
import com.orbitalhq.query.history.SankeyNodeType
import com.orbitalhq.query.history.VyneHistoryRecord
import com.orbitalhq.schemas.fqn
import org.junit.Test
import org.springframework.core.ResolvableType
import org.springframework.core.io.buffer.NettyDataBufferFactory
import org.springframework.util.MimeType
import java.time.Instant
import java.time.temporal.ChronoUnit

class VyneHistoryRecordObjectEncoderTest {
   private val dataBufferFactory = NettyDataBufferFactory(PooledByteBufAllocator())
   private val cborMimeType = MimeType.valueOf("application/cbor")
   private val resolvableType = ResolvableType.forType(VyneHistoryRecord::class.java)
   private val encoder = VyneHistoryRecordObjectEncoder()
   private val decoder = VyneHistoryRecordDecoder()

   @Test
   fun `FlowChartData is serializable`() {
      val chartRow = QuerySankeyChartRow(
         queryId = "queryId",
         sourceNode = "source",
         sourceNodeType = SankeyNodeType.AttributeName,
         targetNode = "target",
         targetNodeType = SankeyNodeType.Expression, count = 100)

      val flowChartData = FlowChartData(listOf(chartRow), "queryId")
      val encodedValue = encoder.encodeValue(
         flowChartData,
         dataBufferFactory,
         resolvableType,
         cborMimeType,
         mutableMapOf())

      val decodedFlowChartData = decoder.decode(encodedValue, resolvableType, cborMimeType, mutableMapOf())
      decodedFlowChartData.should.equal(flowChartData)
   }

   @Test
   fun `LineageRecord is serializable`() {
      val lineageRecord = LineageRecord(
         queryId = "queryId",
         dataSourceId = "datasourceId",
         dataSourceJson = "json",
         dataSourceType = "type"
      )
      val encodedValue = encoder.encodeValue(
         lineageRecord,
         dataBufferFactory,
         resolvableType,
         cborMimeType,
         mutableMapOf())

      val decodedValue = decoder.decode(encodedValue, resolvableType, cborMimeType, mutableMapOf())
      decodedValue.should.equal(lineageRecord)
   }

   @Test
   fun `QueryResultRow is serializable`() {
      val queryResultRow = QueryResultRow(
         queryId = "queryId",
         rowId = 100,
         json = "json",
         valueHash = 1
      )
      val encodedValue = encoder.encodeValue(
         queryResultRow,
         dataBufferFactory,
         resolvableType,
         cborMimeType,
         mutableMapOf())

      val decodedValue = decoder.decode(encodedValue, resolvableType, cborMimeType, mutableMapOf())
      decodedValue.should.equal(queryResultRow)
   }

   @Test
   fun `RemoteCallResponse is serializable`() {
      val remoteCallResponse = RemoteCallResponse(
         queryId = "queryId",
         remoteCallId = "callId",
         response = "response",
         responseId = "responseID",
         startTime = Instant.parse("2022-10-22T23:00:00Z"),
         durationMs = 33,
         exchange = HttpExchange(
            "http://foo.com",
            "GET",
            "{ foo }",
            200,
            20000
         ),
         operation = "foo.bar.Bz".fqn(),
         success = true,
         messageKind = ResponseMessageType.EVENT,
         address = "",
         responseType = "FooResponse".fqn()
      )
      val encodedValue = encoder.encodeValue(
         remoteCallResponse,
         dataBufferFactory,
         resolvableType,
         cborMimeType,
         mutableMapOf())

      val decodedValue = decoder.decode(encodedValue, resolvableType, cborMimeType, mutableMapOf())
      decodedValue.should.equal(remoteCallResponse)
   }

   @Test
   fun `QuerySummary is serializable`() {
      val querySummary = QuerySummary(
         queryId = "queryId",
         clientQueryId = "clientQueryId",
         taxiQl = "findAll...",
         queryJson = null,
         // Instant truncated to millis as our instant serialiser does the same.
         startTime = Instant.now().truncatedTo(ChronoUnit.MILLIS),
         responseStatus = QueryResponse.ResponseStatus.RUNNING,
      )
      val encodedValue = encoder.encodeValue(
         querySummary,
         dataBufferFactory,
         resolvableType,
         cborMimeType,
         mutableMapOf())

      val decodedValue = decoder.decode(encodedValue, resolvableType, cborMimeType, mutableMapOf())
      decodedValue.should.equal(querySummary)
   }
}
