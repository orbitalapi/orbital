package io.vyne.queryService.history.db

import ch.streamly.chronicle.flux.ChronicleStore
import com.google.common.primitives.Ints
import io.vyne.query.history.QueryResultRow
import io.vyne.query.history.RemoteCallResponse
import mu.KotlinLogging
import reactor.core.publisher.Flux
import java.io.File
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

/**
 * A fast disk-based queue which offloads pending history results
 * until the writing layer can keep up.
 * This prevents large heavy queyr history objects being held on-heap
 * when the system is under load.
 *
 * We use Chronicle to stream the contents to disk.
 * A separate queue - meaning a separate directory (with individual reader/writers) is used for
 * each query
 */
class HistoryPersistenceQueue(val queryId: String, val baseQueuePath: Path) {
   val queryBasePath  = baseQueuePath.resolve("$queryId/").toFile().canonicalPath
   private val queryResultRowStore: ChronicleStore<QueryResultRow> =
      ChronicleStore(baseQueuePath.resolve("$queryBasePath/results/").toFile().canonicalPath,
         { queryResultRow -> queryResultRowToBinary(queryResultRow) },
         { bytes -> queryResultRowFromBinary(bytes) }
      )

   private val remoteCallResponseStore: ChronicleStore<RemoteCallResponse> =
      ChronicleStore(baseQueuePath.resolve("$queryBasePath/remote/").toFile().canonicalPath,
         { remoteCallResponse -> remoteCallResponseToBinary(remoteCallResponse) },
         { bytes -> remoteCallResponseFromBinary(bytes) }
      )

   init {
       logger.info { "History queue working in $queryBasePath" }
   }

   fun retrieveNewResultRows(): Flux<QueryResultRow> = queryResultRowStore.retrieveNewValues()
   fun retrieveNewRemoteCalls(): Flux<RemoteCallResponse> = remoteCallResponseStore.retrieveNewValues()

   fun storeResultRow(resultRow: QueryResultRow) {
      queryResultRowStore.store(resultRow)
   }

   fun storeRemoteCallResponse(remoteCallResponse: RemoteCallResponse) {
      remoteCallResponseStore.store(remoteCallResponse)
   }

   /**
    * Convert a QueryResultRow to ByteArray - extremely flaky and change to QueryResultRow
    * will break this
    */
   private fun queryResultRowToBinary(queryResultRow: QueryResultRow): ByteArray {

      val queryId: ByteArray = queryResultRow.queryId.toByteArray(Charsets.UTF_8)
      val valueHash: ByteArray = Ints.toByteArray(queryResultRow.valueHash)
      val json: ByteArray = queryResultRow.json.toByteArray(Charsets.UTF_8)

      val result = ByteArray(queryId.size + json.size + valueHash.size)

      System.arraycopy(queryId, 0, result, 0, queryId.size)
      System.arraycopy(valueHash, 0, result, queryId.size, valueHash.size)
      System.arraycopy(json, 0, result, queryId.size + valueHash.size, json.size)

      return result
   }

   /**
    * Convert a QueryResultRow to ByteArray - extremely flaky and change to RemoteCallResponse
    * will break this
    */
   private fun remoteCallResponseToBinary(remoteCallResponse: RemoteCallResponse): ByteArray {
      val responseId: ByteArray = remoteCallResponse.responseId.toByteArray(Charsets.UTF_8)
      val remoteCallId: ByteArray = remoteCallResponse.remoteCallId.toByteArray(Charsets.UTF_8)
      val queryId: ByteArray = remoteCallResponse.queryId.toByteArray(Charsets.UTF_8)
      val response: ByteArray = remoteCallResponse.response.toByteArray(Charsets.UTF_8)

      val result = ByteArray(responseId.size + remoteCallId.size + queryId.size + response.size)

      System.arraycopy(responseId, 0, result, 0, responseId.size)
      System.arraycopy(remoteCallId, 0, result, 36, remoteCallId.size)
      System.arraycopy(queryId, 0, result, 72, queryId.size)
      System.arraycopy(response, 0, result, 108, response.size)
      return result
   }

   /**
    * Convert a ByteArray to QueryResultRow - extremely flaky and change to QueryResultRow
    * will break this
    */
   private fun queryResultRowFromBinary(bytes: ByteArray): QueryResultRow? {
      val queryId = ByteArray(36) //UUID length
      val valueHash = ByteArray(4) // Size of Int
      val json = ByteArray(bytes.size - 40)  //Rest is json

      System.arraycopy(bytes, 0, queryId, 0, queryId.size)
      System.arraycopy(bytes, 36, valueHash, 0, valueHash.size)
      System.arraycopy(bytes, 40, json, 0, json.size)

      return QueryResultRow(
         queryId = String(queryId, Charsets.UTF_8),
         valueHash = Ints.fromByteArray(valueHash),
         json = String(json, Charsets.UTF_8)
      )
   }

   /**
    * Convert a ByteArray to QueryResultRow - extremely flaky and change to QueryResultRow
    * will break this
    */
   private fun remoteCallResponseFromBinary(bytes: ByteArray): RemoteCallResponse? {

      val responseId = ByteArray(36) //UUID length
      val remoteCallId = ByteArray(36) // UUID length
      val queryId = ByteArray(36)  //UUID length
      val response = ByteArray(bytes.size - 108)  //UUID length

      System.arraycopy(bytes, 0, responseId, 0, responseId.size)
      System.arraycopy(bytes, 36, remoteCallId, 0, remoteCallId.size)
      System.arraycopy(bytes, 72, queryId, 0, queryId.size)
      System.arraycopy(bytes, 108, response, 0, response.size)

      return RemoteCallResponse(
         responseId = String(responseId, Charsets.UTF_8),
         remoteCallId = String(remoteCallId, Charsets.UTF_8),
         queryId = String(queryId, Charsets.UTF_8),
         response = String(response, Charsets.UTF_8)
      )
   }

   fun shutDown() {
      try {
         File(queryBasePath).deleteRecursively()
      } catch (exception: Exception) {
         logger.warn(exception) { "Unable to delete queue directory for query $queryId - ${exception.message}" }
      }
   }
}
