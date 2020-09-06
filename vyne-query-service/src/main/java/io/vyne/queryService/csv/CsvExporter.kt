package io.vyne.queryService.csv

import io.vyne.models.TypeNamedInstance
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import lang.taxi.types.PrimitiveType
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.StringWriter

fun toCsv(results: Map<String, Any?>, schema: Schema): ByteArray {
   val writer = StringWriter()
   val printer = CSVPrinter(writer, CSVFormat.DEFAULT.withFirstRecordAsHeader())
   results.keys.forEach { key ->
      val rowType = getRowType(key,schema)
      when (results[key]) {
         is List<*> -> {
            val listOfObj = results[key]  as List<*>

            if(listOfObj.isNotEmpty()){
               when(listOfObj[0]) {
                  is TypeNamedInstance -> {
                     val rows = results[key]  as List<TypeNamedInstance>
                     printer.printRecord(rowType.attributes.keys)
                     rows.forEach { row ->
                        val attributes = row.value as Map<String,TypeNamedInstance?>
                        printer.printRecord( rowType.attributes.keys.map { fieldName -> attributes[fieldName]?.value } )
                     }
                  }
                  is Map<*, *> -> {
                     val rows = results[key]  as List<Map<String,Any>>
                     printer.printRecord(rowType.attributes.keys)
                     rows.forEach { fields ->
                        printer.printRecord( rowType.attributes.keys.map { fieldName -> fields[fieldName] } )
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

fun getRowType(key: String, schema: Schema): Type {
   val typeName = key.fqn()
   val rowTypeName = if (typeName.fullyQualifiedName == PrimitiveType.ARRAY.qualifiedName) {
      if (typeName.parameters.size == 1) {
         typeName.parameters.first()
      } else {
         TODO("Exporting untyped Arrays is not yet supported")
      }
   } else {
      typeName
   }

   return schema.type(rowTypeName)
}
