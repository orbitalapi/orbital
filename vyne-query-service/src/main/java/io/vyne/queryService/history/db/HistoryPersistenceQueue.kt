package io.vyne.queryService.history.db

import ch.streamly.chronicle.flux.ChronicleStore
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
   val queryBasePath = baseQueuePath.resolve("$queryId/").toFile().canonicalPath
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

   private val objectMapper = jacksonObjectMapper()

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
      return objectMapper.writeValueAsBytes(queryResultRow)
   }

   /**
    * Convert a QueryResultRow to ByteArray - extremely flaky and change to RemoteCallResponse
    * will break this
    */
   private fun remoteCallResponseToBinary(remoteCallResponse: RemoteCallResponse): ByteArray {
      return objectMapper.writeValueAsBytes(remoteCallResponse)
   }

   /**
    * Convert a ByteArray to QueryResultRow - extremely flaky and change to QueryResultRow
    * will break this
    */
   private fun queryResultRowFromBinary(bytes: ByteArray): QueryResultRow {
      return objectMapper.readValue(bytes, QueryResultRow::class.java)
   }

   /**
    * Convert a ByteArray to QueryResultRow - extremely flaky and change to QueryResultRow
    * will break this
    */
   private fun remoteCallResponseFromBinary(bytes: ByteArray): RemoteCallResponse {
      return objectMapper.readValue(bytes, RemoteCallResponse::class.java)
   }

   fun shutDown() {
      try {
         File(queryBasePath).deleteRecursively()
      } catch (exception: Exception) {
         logger.warn(exception) { "Unable to delete queue directory for query $queryId - ${exception.message}" }
      }
   }
}
