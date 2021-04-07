package io.vyne.queryService

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.VersionedSource
import io.vyne.queryService.history.RegressionPackRequest
import io.vyne.schemaStore.SchemaSourceProvider
import io.vyne.schemaStore.VersionedSourceProvider
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import reactor.core.publisher.Mono

//@Component
class RegressionPackProvider(objectMapper: ObjectMapper,
//                             private val history: QueryHistory,
                             private val schemaProvider: SchemaSourceProvider) {
   private val objectWriter = objectMapper.writerWithDefaultPrettyPrinter()

   fun createRegressionPack(request: RegressionPackRequest): Mono<StreamingResponseBody> {
      TODO()
//      return history
//         .get(request.queryId)
//         .map { queryHistoryRecord -> toRegressionPack(request, queryHistoryRecord) }
   }

   private fun toRegressionPack(request: RegressionPackRequest, queryHistoryRecord: Any): StreamingResponseBody {
      TODO()
//      val filenameSafeSpecName = request.regressionPackName.replace(" ", "-")
//      val historyRecordFileName = "history-record.json"
//      val schemaFileName = "schema.json"
//
//      val directoryName = "$filenameSafeSpecName/"
//
//      val contentPairs = listOf(
//         null to ZipEntry(directoryName),
//         objectWriter.writeValueAsBytes(queryHistoryRecord) to ZipEntry(directoryName + historyRecordFileName),
//         objectWriter.writeValueAsBytes(getVersionedSchemas()) to ZipEntry(directoryName + schemaFileName)
//      )
//
//      return StreamingResponseBody { outputStream ->
//         val zipStream = ZipOutputStream(outputStream)
//         contentPairs.forEach { (byteArray, zipParameters) ->
//            zipStream.putNextEntry(zipParameters)
//            if (byteArray != null) {
//               zipStream.write(byteArray)
//            }
//            zipStream.closeEntry()
//         }
//         zipStream.close()
//      }
   }

   private fun getVersionedSchemas(): List<VersionedSource> {
      return if (schemaProvider is VersionedSourceProvider) {
         schemaProvider.versionedSources.sortedBy { it.name }
      } else {
         emptyList()
      }
   }
}
