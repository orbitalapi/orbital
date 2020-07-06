package io.vyne.queryService

import com.fasterxml.jackson.databind.ObjectMapper
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

   private fun toCsv(results: Map<String, Any?>): ByteArray {
      val writer = StringWriter()
      val printer = CSVPrinter(writer, CSVFormat.DEFAULT.withFirstRecordAsHeader())
      results.keys.forEach {
         val values = (results[it] as List<*>)
         printer.printRecord(it)
         printer.printRecord((values.first() as Map<*, *>).keys)
         values.forEach { fields ->
            printer.printRecord((fields as Map<*, *>).values)
         }
      }
      return writer.toString().toByteArray()
   }

   private fun toJson(results: Map<String, Any?>): ByteArray {
      return objectMapper.writeValueAsBytes(results)
   }
}

enum class ExportType {
   JSON, CSV
}
