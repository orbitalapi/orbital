package io.vyne.queryService

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.springframework.stereotype.Component
import java.io.StringWriter
import javax.activation.UnsupportedDataTypeException

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
         when (results[it]) {
            is List<*> -> {
               val listOfObj = results[it] as List<Map<*, *>>
               printer.printRecord(listOfObj.first().keys)
               listOfObj.forEach { fields ->
                  printer.printRecord(fields.values)
               }
            }
            is Map<*, *> -> {
               val singleObj = results[it] as Map<*, *>
               printer.printRecord(singleObj.keys)
               printer.printRecord(singleObj.values)
            }
         }
      }
      return writer.toString().toByteArray()
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
