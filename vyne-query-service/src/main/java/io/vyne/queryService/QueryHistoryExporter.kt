package io.vyne.queryService

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.models.TypeNamedInstance
import io.vyne.queryService.csv.toCsv
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.springframework.stereotype.Component
import java.io.StringWriter

@Component
class QueryHistoryExporter(private val objectMapper: ObjectMapper) {
   fun export(results: Map<String, Any?>, type: ExportType): ByteArray {
      return when (type) {
         ExportType.CSV -> toCsv(results)
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
