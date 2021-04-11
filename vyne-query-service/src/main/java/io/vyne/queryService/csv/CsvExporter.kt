package io.vyne.queryService.csv

import io.vyne.models.TypeNamedInstance
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import lang.taxi.types.ArrayType
import me.eugeniomarletti.kotlin.metadata.shadow.utils.addToStdlib.safeAs
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.http4k.asByteBuffer
import java.io.CharArrayWriter
import java.io.File
import java.io.StringWriter
import java.util.concurrent.atomic.AtomicInteger


fun toCsv(results: Flow<TypeNamedInstance>, schema: Schema): Flow<CharSequence> {



   fun toCharSequence(values: Set<Any?>): CharSequence {
      val charWriter = CharArrayWriter()
      val printer = CSVPrinter(charWriter, CSVFormat.DEFAULT)
      printer.printRecord(values)
      return charWriter.toString()
   }

   return results
      .withIndex()
      .flatMapConcat { indexedValue ->
         val typeNamedInstance = indexedValue.value
         val index = indexedValue.index
         val raw = typeNamedInstance.convertToRaw().safeAs<Map<String, Any>>()
            ?: error("Export is only supported on map types currently")
         val includeHeaders = index == 0;
         val type = schema.type(typeNamedInstance.typeName)
         val values = toCharSequence(type.attributes.keys.map { fieldName -> raw[fieldName] }
            .toSet())
         if (includeHeaders) {
            val headers = toCharSequence(type.attributes.keys)
            flowOf(headers, values)
         } else {
            flowOf(values)
         }

      }
}

/*
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
   val rowTypeName = if (typeName.fullyQualifiedName == ArrayType.NAME) {
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
*/



@FlowPreview
fun toCsv(results: Flow<TypedInstance>): Flow<CharSequence> {

   val writer = StringWriter()

   runBlocking {
      val res = results.toList()


      val printer = CSVPrinter(writer, CSVFormat.DEFAULT.withFirstRecordAsHeader())

      printer.printRecord(res.get(0).type.attributes.keys)
      res.forEach {

            when (it) {
               is TypedObject -> {
                  try {
                     printer.printRecord(it.type.attributes.keys.map { fieldName -> it[fieldName].value })
                  } catch (exception:Exception) {
                     println("Error writing to CSV ${exception.message}")
                  }
               }
               else -> println("writeCsvRecord is not supported for typedInstance of type ${it::class.simpleName}")
            }
      }



   }

   return writer.toString().lines().asFlow().map { "$it\n" }
/*
   val indexTrcker = AtomicInteger(0)

   val charWriter = CharArrayWriter()
   val printer = CSVPrinter(charWriter, CSVFormat.DEFAULT)

   fun toCharSequence(typedInstance: Set<Any?>): CharSequence {
      printer.printRecord(typedInstance)
      return ""
   }

   return results.flatMapConcat { typedInstance ->
      when (typedInstance) {
         is TypedObject -> flowOf(typedInstance)
         is TypedCollection -> typedInstance.value.asFlow()
         else -> TODO("Csv support for TypedInstance of type ${typedInstance::class.simpleName} not yet supported")
      }
         .withIndex()
         .flatMapConcat {
            when (indexTrcker.incrementAndGet()) {
               1 -> {

                  flowOf(
                     toCharSequence(it.value.type!!.attributes.keys),
                     toCharSequence(it.value.type.attributes.keys.map { fieldName -> (it.value as TypedObject)[fieldName].value }
                        .toSet())
                  )
               }
               else -> {
                  when (it.value) {
                     is TypedObject -> {
                        flowOf(toCharSequence(typedInstance.type.attributes.keys.map { fieldName -> (it.value as TypedObject)[fieldName].value }
                           .toSet()))
                     }
                     else -> TODO("writeCsvRecord is not supported for typedInstance of type ${it.value::class.simpleName}")
                  }
               }
            }
         }
   }

 */
}

