package com.orbitalhq.cockpit.core.csv

import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.query.QueryResultSerializer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.util.concurrent.atomic.AtomicInteger


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
                        printer.printRecord(it.type!!.attributes.keys) //The header
                        printer.printRecord((queryResultSerializer.serialize(it) as Map<*, *>).map { e -> e.value })
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
                        printer.printRecord((queryResultSerializer.serialize(it) as Map<*, *>).map { e -> e.value })
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
