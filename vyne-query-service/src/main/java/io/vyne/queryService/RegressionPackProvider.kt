package io.vyne.queryService

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.VersionedSource
import io.vyne.query.QuerySpecTypeNode
import io.vyne.query.history.QuerySummary
import io.vyne.queryService.history.RegressionPackRequest
import io.vyne.schemaStore.SchemaSourceProvider
import io.vyne.schemaStore.VersionedSourceProvider
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import reactor.core.publisher.Mono
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Component
class RegressionPackProvider(
   objectMapper: ObjectMapper,
   private val schemaProvider: SchemaSourceProvider
) {
   private val objectWriter = objectMapper.writerWithDefaultPrettyPrinter()

   fun createRegressionPack(results: List<String>, querySummary: QuerySummary, request: RegressionPackRequest): ByteArrayOutputStream {

         val response = mapOf(
            "results" to results,
            "unmatchedNodes" to emptyList<QuerySpecTypeNode>(),
            "fullyResolved" to true,
            "queryResponseId" to querySummary.queryId,
            "responseStatus" to "COMPLETED",
            //"remoteCalls" to [],
            "timings" to mapOf(
               "REMOTE_CALL" to 1044,
               "ROOT" to 6515
            ),
            "error" to querySummary.errorMessage,
            "resultSize" to querySummary.recordCount,
            "durationMs" to querySummary.durationMs
         )

         val queryHistoryRecord = mapOf(
            "className" to "",
            "query" to querySummary.queryJson,
            "response" to response,
            "timstamp" to querySummary.startTime,
            "id" to querySummary.queryId
         )

         val filenameSafeSpecName = request.regressionPackName.replace(" ", "-")
         val historyRecordFileName = "history-record.json"
         val schemaFileName = "schema.json"

         val directoryName = "$filenameSafeSpecName/"

         val contentPairs = listOf(
            null to ZipEntry(directoryName),
            objectWriter.writeValueAsBytes(queryHistoryRecord) to ZipEntry(directoryName + historyRecordFileName),
            objectWriter.writeValueAsBytes(getVersionedSchemas()) to ZipEntry(directoryName + schemaFileName)
         )

         val outputStream = ByteArrayOutputStream()
         val zipStream = ZipOutputStream(outputStream)
         contentPairs.forEach { (byteArray, zipParameters) ->
            zipStream.putNextEntry(zipParameters)
            if (byteArray != null) {
               zipStream.write(byteArray)
            }
            zipStream.closeEntry()
         }
         zipStream.close()

         return outputStream

   }



   private fun getVersionedSchemas(): List<VersionedSource> {
      return if (schemaProvider is VersionedSourceProvider) {
         schemaProvider.versionedSources.sortedBy { it.name }
      } else {
         emptyList()
      }
   }
}
