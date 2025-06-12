package com.orbitalhq.history

import com.orbitalhq.history.chronicle.ChronicleStore
import com.orbitalhq.query.history.LineageRecord
import com.orbitalhq.query.history.QueryErrorEventRow
import com.orbitalhq.query.history.QueryResultRow
import com.orbitalhq.query.history.RemoteCallResponse
import com.orbitalhq.query.history.tracing.TraceEventRow
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import mu.KotlinLogging
import reactor.core.publisher.Flux
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.deleteExisting

private val logger = KotlinLogging.logger {}

fun Path.deleteRecursively() {
   Files.walkFileTree(
      this,
      object : SimpleFileVisitor<Path>() {
         override fun visitFile(file: Path, attrs: BasicFileAttributes?): FileVisitResult {
            file.deleteExisting()
            return FileVisitResult.CONTINUE
         }

         override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
            if (exc != null)
               throw exc
            dir.deleteExisting()
            return FileVisitResult.CONTINUE
         }
      }
   )
}

/**
 * A fast disk-based queue which offloads pending history results
 * until the writing layer can keep up.
 * This prevents large heavy query history objects being held on-heap
 * when the system is under load.
 *
 * We use Chronicle to stream the contents to disk.
 * A separate queue - meaning a separate directory (with individual reader/writers) is used for
 * each query.
 *
 * Design choices:  Using CBOR as the format to persist to disk.
 * We were previously using JSON.  However, the objects being persisted contain
 * nested JSON strings, stored as String, rather Map<>, using @JsonRawValue
 * This is needed for the UI, who expects to receive Json in these spaces.
 *
 * However, Jackson contains a bug, where any object that contains a JsonRawValue
 * cannot be marshalled / unmarshalled successfully:
 * ie: This line throws an exception if the thing contains a @JsonRawValue field:
 *
 * jacksonObjectMapper.readValue(jacksonObjectMapper.writeValueAsString(thingWithNestedJson))
 *
 * Therefore, we can't use Jackson to write the json to disk.  We don't really need JSON here, we just need
 * a robust way to convert objects to/from bytes.  So, using kotlinx.serialization, and chose CBOR for the format.
 * Would've used Json, but kotlinx-json 1.1 doesn't support foo.toByteArray() or foo.fromByteArray()
 * (that's only in 1.2)
 *
 * Using CBOR seems to work well, and has small performance improvements over json
 */
class HistoryPersistenceQueue(val queryId: String, val baseQueuePath: Path) : QueryObservabilityWriter {

   val queryBasePath: String = baseQueuePath.resolve("$queryId/").toFile().canonicalPath

   private val queryResultRowStore: ChronicleStore<QueryResultRow>
   private val remoteCallResponseStore: ChronicleStore<RemoteCallResponse>
   private val lineageRecordStore: ChronicleStore<LineageRecord>
   private val errorEventStore: ChronicleStore<QueryErrorEventRow>
   private val traceEventsStore: ChronicleStore<TraceEventRow>

   init {
      queryResultRowStore =
         ChronicleStore(baseQueuePath.resolve("$queryBasePath/results/").toFile().canonicalPath,
            { queryResultRow -> queryResultRowToByteArray(queryResultRow) },
            { bytes -> queryResultRowFromByteArray(bytes) }
         )

      remoteCallResponseStore =
         ChronicleStore(baseQueuePath.resolve("$queryBasePath/remote/").toFile().canonicalPath,
            { remoteCallResponse -> remoteCallResponseToByteArray(remoteCallResponse) },
            { bytes -> remoteCallResponseFromByteArray(bytes) }
         )

      lineageRecordStore =
         ChronicleStore(baseQueuePath.resolve("$queryBasePath/lineage/").toFile().canonicalPath,
            { lineageRecord -> lineageRecordToByteArray(lineageRecord) },
            { bytes -> lineageRecordFromByteArray(bytes) }
         )
      errorEventStore =
         ChronicleStore(baseQueuePath.resolve("$queryBasePath/errors/").toFile().canonicalPath,
            { errorEvent -> errorEventRowToByteArray(errorEvent) },
            { bytes -> errorEventRowFromByteArray(bytes) }
         )

      traceEventsStore = ChronicleStore(baseQueuePath.resolve("$queryBasePath/traceEvents/").toFile().canonicalPath,
         { tracingEvent -> traceEventToByteArray(tracingEvent) },
         { bytes -> traceEventFromByteArray(bytes) }
      )
      logger.info { "History queue working in $queryBasePath" }
   }

   fun retrieveNewResultRows(): Flux<QueryResultRow> = queryResultRowStore.retrieveNewValues()
   fun retrieveNewRemoteCalls(): Flux<RemoteCallResponse> = remoteCallResponseStore.retrieveNewValues()
   fun retrieveNewLineageRecords(): Flux<LineageRecord> = lineageRecordStore.retrieveNewValues()
   fun retrieveNewErrorEvents(): Flux<QueryErrorEventRow> = errorEventStore.retrieveNewValues()
   fun retrieveTraceEvents(): Flux<TraceEventRow> = traceEventsStore.retrieveNewValues()

   override fun storeResultRow(resultRow: QueryResultRow) {
      queryResultRowStore.store(resultRow)
   }

   override fun storeRemoteCallResponse(remoteCallResponse: RemoteCallResponse) {
      remoteCallResponseStore.store(remoteCallResponse)
   }

   override fun storeLineageRecord(lineageRecord: LineageRecord) {
      lineageRecordStore.store(lineageRecord)
   }

   override fun storeErrorEvent(event: QueryErrorEventRow) {
      errorEventStore.store(event)
   }

   override fun storeTraceEvent(event: TraceEventRow) {
      traceEventsStore.store(event)
   }
   private fun traceEventToByteArray(traceEvent: TraceEventRow): ByteArray {
      return Cbor.encodeToByteArray(traceEvent)
   }
   private fun traceEventFromByteArray(byteArray: ByteArray): TraceEventRow {
      return Cbor.decodeFromByteArray(byteArray)
   }
   private fun errorEventRowToByteArray(eventRow: QueryErrorEventRow): ByteArray {
      return Cbor.encodeToByteArray(eventRow)
   }

   private fun errorEventRowFromByteArray(byteArray: ByteArray): QueryErrorEventRow {
      return Cbor.decodeFromByteArray(byteArray)
   }
   private fun queryResultRowToByteArray(queryResultRow: QueryResultRow): ByteArray {
      return Cbor.encodeToByteArray(queryResultRow)
   }

   private fun remoteCallResponseToByteArray(remoteCallResponse: RemoteCallResponse): ByteArray {
      return Cbor.encodeToByteArray(remoteCallResponse)
   }

   private fun queryResultRowFromByteArray(bytes: ByteArray): QueryResultRow {
      return Cbor.decodeFromByteArray(bytes)
   }

   private fun remoteCallResponseFromByteArray(bytes: ByteArray): RemoteCallResponse {
      return Cbor.decodeFromByteArray(bytes)
   }

   private fun lineageRecordToByteArray(lineageRecord: LineageRecord): ByteArray {
      return Cbor.encodeToByteArray(lineageRecord)
   }

   private fun lineageRecordFromByteArray(bytes: ByteArray): LineageRecord {
      return Cbor.decodeFromByteArray(bytes)
   }

   fun shutDown() {
      lineageRecordStore.close()
      queryResultRowStore.close()
      remoteCallResponseStore.close()
      try {
         Path.of(queryBasePath).deleteRecursively()
      } catch (exception: Exception) {
         logger.warn(exception) { "Unable to delete queue directory for query $queryId - ${exception.message}" }
      }
   }
}
