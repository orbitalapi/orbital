package com.orbitalhq.history.rest.export

import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.VersionedSource
import com.orbitalhq.history.api.RegressionPackRequest
import com.orbitalhq.models.TypeNamedInstance
import com.orbitalhq.query.history.LineageRecord
import com.orbitalhq.query.history.QuerySummary
import com.orbitalhq.query.history.RemoteCallResponse
import com.orbitalhq.schema.api.SchemaSourceProvider
import com.orbitalhq.schema.api.ParsedSourceProvider
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Component
class RegressionPackProvider(
   objectMapper: ObjectMapper,
   private val schemaProvider: SchemaSourceProvider
) {
   private val objectWriter = objectMapper.writerWithDefaultPrettyPrinter()

   fun createRegressionPack(results: List<TypeNamedInstance>, querySummary: QuerySummary, lineageRecords: List<LineageRecord>, remoteCalls: List<RemoteCallResponse>, request: RegressionPackRequest): ByteArrayOutputStream {

      val filenameSafeSpecName = request.regressionPackName.replace(" ", "-")
      val resultsFilename = "query-results.json"
      val querySummaryFileName = "query-summary.json"
      val lineageRecordsFileName = "lineage-records.json"
      val remoteCallsFileName = "remote-calls.json"


      val schemaFileName = "schema.json"
      val directoryName = "$filenameSafeSpecName/"

      val contentPairs = listOf(
         null to ZipEntry(directoryName),

         objectWriter.writeValueAsBytes(results) to ZipEntry(directoryName + resultsFilename),
         objectWriter.writeValueAsBytes(querySummary) to ZipEntry(directoryName + querySummaryFileName),
         objectWriter.writeValueAsBytes(lineageRecords) to ZipEntry(directoryName + lineageRecordsFileName),
         objectWriter.writeValueAsBytes(remoteCalls) to ZipEntry(directoryName + remoteCallsFileName),
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
      return if (schemaProvider is ParsedSourceProvider) {
         schemaProvider.versionedSources.sortedBy { it.name }
      } else {
         emptyList()
      }
   }
}
