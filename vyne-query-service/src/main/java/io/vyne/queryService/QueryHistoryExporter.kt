package io.vyne.queryService

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.models.TypeNamedInstance
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
         when (results[it]) {
            is List<*> -> {
               val listOfObj = results[it]  as List<*>

               if(!listOfObj.isEmpty()){
                  when(listOfObj[0]) {
                     is TypeNamedInstance -> {
                        val listOfObj = results[it]  as List<TypeNamedInstance>

                        val objs =listOfObj.map { it.value as Map<*,TypeNamedInstance> }.sortedByDescending { it.keys.size }
                        val firstObj = objs[0]
                        val columns = firstObj.keys
                        printer.printRecord(columns)
                        objs.forEach { fields ->
                           printer.printRecord( columns.map { column -> fields[column]?.value })
                        }
                     }
                     is Map<*, *> -> {
                        val listOfObj = results[it]  as List<Map<*, *>>
                        printer.printRecord(listOfObj.first().keys)
                        listOfObj.forEach { fields ->
                           printer.printRecord(fields.values)
                        }
                     }
                  }
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
