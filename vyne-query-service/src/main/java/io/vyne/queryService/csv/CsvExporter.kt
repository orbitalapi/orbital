package io.vyne.queryService.csv

import io.vyne.models.TypeNamedInstance
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.StringWriter

fun toCsv(results: Map<String, Any?>): ByteArray {
   val writer = StringWriter()
   val printer = CSVPrinter(writer, CSVFormat.DEFAULT.withFirstRecordAsHeader())
   results.keys.forEach { key ->
      when (results[key]) {
         is List<*> -> {
            val listOfObj = results[key]  as List<*>

            if(listOfObj.isNotEmpty()){
               when(listOfObj[0]) {
                  is TypeNamedInstance -> {
                     val listOfObj = results[key]  as List<TypeNamedInstance>

                     val objs =listOfObj.map { it.value as Map<*, TypeNamedInstance> }.sortedByDescending { it.keys.size }
                     val firstObj = objs[0]
                     val columns = firstObj.keys
                     printer.printRecord(columns)
                     objs.forEach { fields ->
                        printer.printRecord( columns.map { column -> fields[column]?.value })
                     }
                  }
                  is Map<*, *> -> {
                     val listOfObj = results[key]  as List<Map<*, *>>
                     printer.printRecord(listOfObj.first().keys)
                     listOfObj.forEach { fields ->
                        printer.printRecord(fields.values)
                     }
                  }
               }
            }
         }
         is Map<*, *> -> {
            val singleObj = results[key] as Map<*, *>
            printer.printRecord(singleObj.keys)
            printer.printRecord(singleObj.values)
         }
      }
   }
   return writer.toString().toByteArray()
}
