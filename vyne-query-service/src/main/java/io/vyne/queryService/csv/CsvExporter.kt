package io.vyne.queryService.csv

import io.vyne.models.TypeNamedInstance
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.schemas.Schema
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import me.eugeniomarletti.kotlin.metadata.shadow.utils.addToStdlib.safeAs
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.CharArrayWriter


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

@FlowPreview
fun toCsv(results: Flow<TypedInstance>): Flow<CharSequence> {

   fun toCharSequence(typedInstance: Set<Any?>): CharSequence {
      val charWriter = CharArrayWriter()
      val printer = CSVPrinter(charWriter, CSVFormat.DEFAULT)
      printer.printRecord(typedInstance)
      return charWriter.toString()
   }

   return results.flatMapConcat { typedInstance ->
      when (typedInstance) {
         is TypedObject -> flowOf(typedInstance)
         is TypedCollection -> typedInstance.value.asFlow()
         else -> TODO("Csv support for TypedInstance of type ${typedInstance::class.simpleName} not yet supported")
      }
         .withIndex()
         .flatMapConcat {
            when (it.index) {
               0 -> {
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
}
