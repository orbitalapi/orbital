package io.vyne.queryService.history.db

import com.winterbe.expekt.should
import io.vyne.query.history.QueryResultRow
import io.vyne.query.history.RemoteCallResponse
import io.vyne.utils.Benchmark
import mu.KotlinLogging
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import reactor.kotlin.test.test
import java.nio.file.Paths
import java.util.*

private val logger = KotlinLogging.logger {}
class HistoryPersistenceQueueTest {
   @Rule
   @JvmField
   val tempDir = TemporaryFolder()


   @Test
   fun `can read and write to remote call response queue`() {
      val largeString = (0 until 100000).joinToString(separator = "") { "1" }
      sendAndReceive(
         QueryResultRow(
            null,
            "query-123",
            largeString,
            100293949
         )
      )
   }

   @Test
   fun `can read result row with row id`() {
      val largeString = (0 until 100000).joinToString(separator = "") { "1" }
      sendAndReceive(
         QueryResultRow(
            123002L,
            "query-123",
            largeString,
            100293949
         )
      )
   }

   @Test
   fun `can read result row with negative value hash`() {
      val largeString = (0 until 100000).joinToString(separator = "") { "1" }
      sendAndReceive(
         QueryResultRow(
            123002L,
            "query-123",
            largeString,
            -100293949
         )
      )
   }


   @Test
   fun `can read result row with empty response`() {
      sendAndReceive(
         QueryResultRow(
            123002L,
            "query-123",
            "",
            -100293949
         )
      )
   }

   @Test
   fun `can read remote large call response`() {
      val largeString = (0 until 100000).joinToString(separator = "") { "1" }
      sendAndReceive(RemoteCallResponse(
         "response-id",
         "remote-call-id",
         "query-id",
         largeString
      ))
   }

   @Test
   fun `can read remote call response`() {
      sendAndReceive(RemoteCallResponse(
         "response-id",
         "remote-call-id",
         "query-id",
         """{"icapOrderId":"2020102801000000000000007754710F_1","entryType":"FILL","orderDateTime":1603871617.912185000,"orderDate":1603843200000,"venueOrderStatus":"FILL","cfiCode":"JFTXFP","identifierValue":"EZ91WZD7WC90","tempCfiCode":"JFTXFP","tempCfiCodeForIdentType1Char":"J","tempCfiCodeForIdentType2Char":"JF","tempCfiCodeForIdentType3Char":"JFT","tempCfiCodeForIdentTypeMid":"X","identifierType":"ICAPCCYPAIR","isin":"EZ91WZD7WC90","subSecurityType":"FWD.JPY.USD.1D","priceAmount":104.2,"stopPrice":null,"priceType":"BAPO","requestedQuantity":37000000000,"cumulativeQuantity":37000000000,"remainingQuantity":0,"displayedQuantity":0,"quantityNotation":"MONE","quantityCurrency":"JPY","unitMultiplier":1,"orderType":"Limit","buySellIndicator":"BUYI","orderValidityPeriod":"IOCV","exchange":"XOFF","sourceSystem":"XOFF","tempPayReceive":"JFTXFP-BUYI","leg1PayReceive":null,"leg2PayReceive":null,"tempLegRate":"JFTXFP-BUYI","leg1Rate":null,"leg2Rate":null,"trader":null,"cacibTraderBrokerLogin":null,"brokerVenue":"XOFF","underlyingIdentifierType":"ISIN","underlyingIdentifierValue":null,"tempLegs":"JFTXFP","leg1NotionalValue":null,"leg1OrigCurrNotionalAmount":null,"leg2NotionalValue":null,"leg2OrigCurrNotionalAmount":null,"leg2Currency":null,"method":"GUI","activityCategory":"Hedge","clientid":"SC0000041353","counterpartyLei":"21380076S228I25PD704","counterParty":"ICAP EUROPE LIMITED","cacibLei":"1VUV7VQFKUOQSJ21A208","tempTradeActivityType":"Central Limit Order Book","tradeActivityType":"OTH","brokerName":"icap","caskmessageid":"b79b7aa7-16a4-43e1-ba99-5e6759618691","cask_raw_id":"834e691f-fd77-4780-bf83-57868204eb2d"} """
      ))
   }

   @Test
   fun `can read remote call response with empty response`() {
      sendAndReceive(RemoteCallResponse(
         "response-id",
         "remote-call-id",
         "query-id",
         ""
      ))
   }

   @Test
   fun `after shutdown all contents are cleared`() {
      val largeString = (0 until 100000).joinToString(separator = "") { "1" }
      val queue = sendAndReceive(
         QueryResultRow(
            123002L,
            "62e8ae4d-3a7a-4fe3-8ba1-854c94372586",
            largeString,
            -100293949
         ),
         queryId = "62e8ae4d-3a7a-4fe3-8ba1-854c94372586",
         shutdownAfterCompleted = false
      )
      Paths.get(queue.queryBasePath).toFile().exists().should.be.`true`
      queue.shutDown()
      Paths.get(queue.queryBasePath).toFile().exists().should.be.`false`
   }

   @Test
   fun benchmark() {
      // NOte: I've changed the size of the large string to ~1kb, since we emit
      // 5k of these.  Using 100k (as we do in other tests) created 500mb per benchmarh run!
      val largeString = (0 until 1000).joinToString(separator = "") { "1" }
      Benchmark.benchmark("Sending result rows", warmup = 10, iterations = 30) {
         val queue = HistoryPersistenceQueue(UUID.randomUUID().toString(), tempDir.root.toPath())
         repeat((0 until 5000).count()) {
            queue.storeResultRow(   QueryResultRow(
               it.toLong(),
               "query-123",
               largeString,
               -100293949
            ))
         }
         logger.info { "Completed sending" }
         queue.retrieveNewResultRows()
            .test()
            .expectNextCount(5000)
            .thenCancel()

         queue.shutDown()
      }
   }


   private fun sendAndReceive(response: RemoteCallResponse, queryId: String = UUID.randomUUID().toString(),  shutdownAfterCompleted:Boolean = true): HistoryPersistenceQueue {
      val queue = HistoryPersistenceQueue(queryId, tempDir.root.toPath())

      queue.retrieveNewRemoteCalls()
         .test()
         .then {
            queue.storeRemoteCallResponse(response)
         }
         .expectNext(response)
         .thenCancel()
         .verify()
      if (shutdownAfterCompleted) {
         queue.shutDown()
      }
      return queue
   }
   private fun sendAndReceive(resultRow: QueryResultRow, queryId: String = UUID.randomUUID().toString(), shutdownAfterCompleted:Boolean = true): HistoryPersistenceQueue {
      val queue = HistoryPersistenceQueue(queryId, tempDir.root.toPath())
      queue.retrieveNewResultRows()
         .test()
         .then {
            queue.storeResultRow(resultRow)
         }
         .expectNext(resultRow)
         .thenCancel()
         .verify()

      if (shutdownAfterCompleted) {
         queue.shutDown()
      }
      return queue
   }
}
