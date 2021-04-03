package io.vyne.queryService.history

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.schemaStore.SchemaProvider
import org.springframework.stereotype.Component

@Component
class QueryHistoryExporter(private val objectMapper: ObjectMapper, private val schemaProvider: SchemaProvider) {
   fun export(results: Map<String, Any?>, type: ExportType): ByteArray {
      val schema = schemaProvider.schema()
      return when (type) {
         ExportType.CSV -> error("CSV Export not working") //toCsv(results, schema)
         ExportType.JSON -> toJson(results)
      }
   }

   private fun toJson(results: Map<String, Any?>): ByteArray {
      val json = objectMapper.writeValueAsString(results)
      return objectMapper.writeValueAsString(results)
         .removeRange(
            json.indexOfFirst { it == '{' },
            json.indexOfFirst { it == ':' } + 1)
         .removeSuffix("}")
         .toByteArray()
   }
}

enum class ExportType {
   JSON, CSV
}
