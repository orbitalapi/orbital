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
   fun `can read remote call response`() {
      val largeString = (0 until 100000).joinToString(separator = "") { "1" }
      sendAndReceive(RemoteCallResponse(
         "response-id",
         "remote-call-id",
         "query-id",
         largeString
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
      queue.storeRemoteCallResponse(response)

      queue.retrieveNewRemoteCalls()
         .test()
         .expectNext(response)
         .thenCancel()
      if (shutdownAfterCompleted) {
         queue.shutDown()
      }
      return queue
   }
   private fun sendAndReceive(resultRow: QueryResultRow, queryId: String = UUID.randomUUID().toString(), shutdownAfterCompleted:Boolean = true): HistoryPersistenceQueue {
      val queue = HistoryPersistenceQueue(queryId, tempDir.root.toPath())
      queue.storeResultRow(resultRow)

      queue.retrieveNewResultRows()
         .test()
         .expectNext(resultRow)
         .thenCancel()

      if (shutdownAfterCompleted) {
         queue.shutDown()
      }
      return queue
   }
}
