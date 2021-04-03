package io.vyne.queryService.csv

import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import lang.taxi.types.ArrayType
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter


/**
 * Bridge between the java Appenable class (which streams strings)
 * to a Flow<String>
 */
private class FlowEmittingAppendable(private val flowCollector: FlowCollector<CharSequence>) : Appendable {
   override fun append(charSequence: CharSequence): Appendable {
      // TODO : Is there a non-blocking way to do this?
      runBlocking { flowCollector.emit(charSequence) }
      return this
   }

   override fun append(charSequence: CharSequence, start: Int, end: Int): Appendable {
      // TODO : Is there a non-blocking way to do this?
      runBlocking { flowCollector.emit(charSequence.subSequence(start, end)) }
      return this
   }

   override fun append(char: Char): Appendable {
      // TODO : Is there a non-blocking way to do this?
      runBlocking { flowCollector.emit(char.toString()) }
      return this
   }

}

fun toCsv(results: Flow<TypedInstance>, schema: Schema): Flow<CharSequence> {

   // MP : This is a brain dump of how to rewrite the original code below
   // to tidy it up, and make it make sense in a flow-based world.
   // I haven't tested this at all yet.
   return flow<CharSequence> {
      val flowAppendable = FlowEmittingAppendable(this)
      val printer = CSVPrinter(flowAppendable, CSVFormat.DEFAULT.withFirstRecordAsHeader())
      // For now, we use the first row to infer the type.
      // Polymorphic results in a CSV don't really make much sense, so that's probably ok.
      var rowType: Type? = null
      results.collect { typedInstance ->
         if (typedInstance is TypedCollection) { // Can this happen?
            TODO("Support typed collection to CSV")
         }
         if (rowType == null) {
            rowType = typedInstance.type
            printer.printRecord(rowType!!.attributes.keys)
         }
         if (typedInstance is TypedObject) {
            val rowValues = rowType!!.attributes.keys.map { fieldName -> typedInstance[fieldName].value }
            printer.printRecord(rowValues)
         }
      }
   }
//   // For now, we use the first row to infer the type.
//   // Polymorphic results in a CSV don't really make much sense, so that's probably ok.
//   var rowType: Type? = null
//   results.forEach { key ->
//      if (rowType == null) {
//         rowType
//      }
//      val rowType = getRowType(key, schema)
//      when (results[key]) {
//         is List<*> -> {
//            val listOfObj = results[key] as List<*>
//
//            if (listOfObj.isNotEmpty()) {
//               when (listOfObj[0]) {
//                  is TypeNamedInstance -> {
//                     val rows = results[key] as List<TypeNamedInstance>
//                     printer.printRecord(rowType.attributes.keys)
//                     rows.forEach { row ->
//                        val attributes = row.value as Map<String, TypeNamedInstance?>
//                        printer.printRecord(rowType.attributes.keys.map { fieldName -> attributes[fieldName]?.value })
//                     }
//                  }
//                  is Map<*, *> -> {
//                     val rows = results[key] as List<Map<String, Any>>
//                     printer.printRecord(rowType.attributes.keys)
//                     rows.forEach { fields ->
//                        printer.printRecord(rowType.attributes.keys.map { fieldName -> fields[fieldName] })
//                     }
//                  }
//               }
//            }
//         }
//         is Map<*, *> -> {
//            val singleObj = results[key] as Map<*, *>
//            printer.printRecord(singleObj.keys)
//            printer.printRecord(singleObj.values)
//         }
//      }
//   }
//   return writer.toString().toByteArray()
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
