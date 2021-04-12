package io.vyne.queryService.csv

import io.vyne.models.TypeNamedInstance
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.queryService.QueryResultSerializer
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
import java.lang.StringBuilder
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

fun toCsv(results: Flow<TypedInstance>, queryResultSerializer: QueryResultSerializer): Flow<CharSequence> {

   val indexTracker = AtomicInteger(0)
   val writer = StringBuilder()
   val printer = CSVPrinter(writer, CSVFormat.DEFAULT.withFirstRecordAsHeader())

   return results.flatMapConcat { typedInstance ->
      when (typedInstance) {
         is TypedObject -> flowOf(typedInstance)
         is TypedCollection -> typedInstance.value.asFlow()
         else -> TODO("Csv support for TypedInstance of type ${typedInstance::class.simpleName} not yet supported")
      }
         .map {
            when (indexTracker.incrementAndGet()) {
               1 -> {

                  when (it) {
                     is TypedObject -> {
                        printer.printRecord(it.type!!.attributes.keys)
                        //printer.printRecord(it.type.attributes.keys.map { fieldName -> (it as TypedObject)[fieldName].value })

                        printer.printRecord((queryResultSerializer.serialize(it) as LinkedHashMap<*, *>).map { e -> e.value})
                        val csvRecord = writer.toString()
                        writer.clear()
                        csvRecord
                     }
                     else -> TODO("writeCsvRecord is not supported for typedInstance of type ${it::class.simpleName}")
                  }

               }
               else -> {
                  when (it) {
                     is TypedObject -> {
                        println("-----------")
                        println("Coverted -- ${queryResultSerializer.serialize(it)}")
                        println("a hasmap = ")
                        println((queryResultSerializer.serialize(it) as LinkedHashMap<*,*>))
                        println("-----------")

                        printer.printRecord( (queryResultSerializer.serialize(it) as LinkedHashMap<*,*>).map { e -> e.value})
                        //printer.printRecord( typedInstance.type.attributes.keys.map { fieldName -> it [fieldName].value } )
                        val csvRecord = writer.toString()
                        writer.clear()
                        csvRecord
                     }
                     else -> TODO("writeCsvRecord is not supported for typedInstance of type ${it::class.simpleName}")
                  }
               }
            }
         }
   }

}

/*
.flatMapMerge { typedInstance ->
   // This is a smell.
   // I've noticed that when projecting, in this point of the code
   // we get individual typed instances.
   // However, if we're not projecting, we get a single
   // typed collection.
   // This meas that the shape of the response (array vs single)
   // varies based on the query, which is incorrect.
   // Therefore, unwrap collections here.
   // This smells, because it could be indicative of a problem
   // higher in the stack.
   if (typedInstance is TypedCollection) {
      typedInstance.map { serializer.serialize(it) }
   } else {
      listOf(serializer.serialize(typedInstance))
   }.filterNotNull()
      .asFlow()
      *
 */
