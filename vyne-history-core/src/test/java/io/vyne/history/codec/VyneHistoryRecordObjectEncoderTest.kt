package io.vyne.history.codec

import com.winterbe.expekt.should
import io.netty.buffer.PooledByteBufAllocator
import io.vyne.query.QueryResponse
import io.vyne.query.history.FlowChartData
import io.vyne.query.history.LineageRecord
import io.vyne.query.history.QueryResultRow
import io.vyne.query.history.QuerySankeyChartRow
import io.vyne.query.history.QuerySummary
import io.vyne.query.history.RemoteCallResponse
import io.vyne.query.history.SankeyNodeType
import io.vyne.query.history.VyneHistoryRecord
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
         responseId = "responseID"
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
