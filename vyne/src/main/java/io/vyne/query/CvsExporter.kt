package io.vyne.query

import io.vyne.models.TypeNamedInstance
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Field
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.withIndex
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.CharArrayWriter

fun toCsv(results: Flow<Pair<TypeNamedInstance, Set<PersistedAnonymousType>>>, schema: Schema): Flow<CharSequence> {


   fun toCharSequence(values: Collection<Any?>): CharSequence {
      val charWriter = CharArrayWriter()
      val printer = CSVPrinter(charWriter, CSVFormat.DEFAULT)
      printer.printRecord(values)
      return charWriter.toString()
   }

   return results
      .withIndex()
      .flatMapConcat { indexedValue ->
         val typeNamedInstance = indexedValue.value.first
         val anonymousTypeDefinitions = indexedValue.value.second
         val index = indexedValue.index
         val raw = typeNamedInstance.convertToRaw() as? Map<String, Any>
            ?: error("Export is only supported on map types currently")
         val includeHeaders = index == 0
         val attributes = if (anonymousTypeDefinitions.isEmpty()) {
            schema.type(typeNamedInstance.typeName).attributes
         } else {
            anonymousTypeDefinitions.first { it.name.fullyQualifiedName ==  typeNamedInstance.typeName}.attributes
         }
         val values = toCharSequence(attributes.keys.map { fieldName -> raw[fieldName] })
         if (includeHeaders) {
            val headers = toCharSequence(attributes.keys)
            flowOf(headers, values)
         } else {
            flowOf(values)
         }
      }
}

/**
 * Anonymous queries has the corresponding io.vyne.schemas.Type persisted. However, persisted data lacks the
 * taxiType field, so we can't deserialise the anonymous Type json back to vyne Type. CSV export requires attribute names vs
 * fully qualified names, so we use below class for deserialisation.
 */
data class PersistedAnonymousType(
   val name: QualifiedName,
   val attributes: Map<AttributeName, Field> = emptyMap()
)
