package io.vyne.query

import io.vyne.models.TypeNamedInstance
import io.vyne.schemas.Schema
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.withIndex
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.CharArrayWriter

fun toCsv(results: Flow<TypeNamedInstance>, schema: Schema): Flow<CharSequence> {

   fun toCharSequence(values: Collection<Any?>): CharSequence {
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
         val raw = typeNamedInstance.convertToRaw() as? Map<String, Any>
            ?: error("Export is only supported on map types currently")
         val includeHeaders = index == 0;
         val type = schema.type(typeNamedInstance.typeName)
         val values = toCharSequence(type.attributes.keys.map { fieldName -> raw[fieldName] })
         if (includeHeaders) {
            val headers = toCharSequence(type.attributes.keys)
            flowOf(headers, values)
         } else {
            flowOf(values)
         }

      }
}

