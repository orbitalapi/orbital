package io.vyne.queryService.history

import com.winterbe.expekt.should
import io.vyne.query.RemoteCall
import io.vyne.query.ResponseCodeGroup
import io.vyne.query.ResponseMessageType
import io.vyne.schemas.fqn
import org.junit.Test
import java.time.Instant

class RemoteCallAnalyzerTest {
   val analyzer = RemoteCallAnalyzer()

   @Test
   fun `basic counts`() {
      val remoteCalls = listOf(
         remoteCall("request1", "request1-reponse1", "operationA", 20),
         remoteCall("request2", "request2-reponse1", "operationA", 30),
         remoteCall("request3", "request3-reponse1", "operationB", 15),
         remoteCall("request4", "request4-reponse1", "operationB", 20),
      )
      val stats = analyzer.generateStats(remoteCalls).groupBy { it.operationName }
      stats["operationA"]!!.first().let { operationAStats ->
         operationAStats.callsInitiated.should.equal(2)
         operationAStats.averageTimeToFirstResponse.toDouble().should.be.closeTo(25.00)
         operationAStats.totalWaitTime.should.equal(50)
         operationAStats.responseCodes.should.equal(
            responseCodeTable(http2xx = 2)
         )
      }
   }

   @Test
   fun `streaming responses dont have wait time`() {
      val remoteCalls = listOf(
         remoteCall("request1", "request1-reponse1", "operationA", 20, responseMessageType = ResponseMessageType.EVENT),
         remoteCall("request2", "request2-reponse1", "operationA", 30, responseMessageType = ResponseMessageType.EVENT)
      )
      val stats = analyzer.generateStats(remoteCalls)
      stats.should.have.size(1)
      stats.first().totalWaitTime.should.be.`null`
   }

   @Test
   fun `when request has multiple responses the lowest response time is used for first response`() {
      val remoteCalls = listOf(
         remoteCall("request1", "request1-reponse1", "operationA", 20),
         remoteCall("request1", "request1-reponse2", "operationA", 30),
         remoteCall("request1", "request1-reponse3", "operationA", 7),
         remoteCall("request1", "request1-reponse4", "operationA", 20),
      )
      val stats = analyzer.generateStats(remoteCalls)
      stats.should.have.size(1)
      stats.first().averageTimeToFirstResponse.toDouble().should.be.closeTo(7.0)
   }

   private fun responseCodeTable(
      http2xx: Int = 0,
      http3xx: Int = 0,
      http4xx: Int = 0,
      http5xx: Int = 0
   ): Map<ResponseCodeGroup, Int> {
      return mapOf(
         ResponseCodeGroup.HTTP_2XX to http2xx,
         ResponseCodeGroup.HTTP_3XX to http3xx,
         ResponseCodeGroup.HTTP_4XX to http4xx,
         ResponseCodeGroup.HTTP_5XX to http5xx,
         ResponseCodeGroup.UNKNOWN to 0
      )

   }


   private fun remoteCall(
      callId: String,
      responseId: String,
      operationName: String,
      durationMs: Int,
      resultCode: Int = 200,
      responseMessageType: ResponseMessageType = ResponseMessageType.FULL,
      serviceName: String = "Service"
   ): RemoteCall {
      return RemoteCall(
         callId,
         responseId,
         serviceName.fqn(),
         "",
         operationName,
         "Something".fqn(),
         "",
         "",
         resultCode,
         durationMs.toLong(),
         Instant.now(),
         responseMessageType,
         null
      )
   }
}
