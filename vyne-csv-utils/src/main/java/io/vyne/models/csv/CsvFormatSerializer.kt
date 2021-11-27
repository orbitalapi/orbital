package io.vyne.models.csv

import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.format.ModelFormatSerializer
import io.vyne.models.format.TypedInstanceInfo
import io.vyne.schemas.Metadata
import io.vyne.schemas.Type
import lang.taxi.accessors.ColumnAccessor
import lang.taxi.types.ObjectType
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.StringWriter

object CsvFormatSerializer : ModelFormatSerializer {
   override fun write(result: TypedInstance, metadata: Metadata, typedInstanceInfo: TypedInstanceInfo): Any? {
      val rawValue = result.toRawObject() ?: return null

      val csvAnnotation = CsvFormatSpecAnnotation.from(metadata)
      val parameters = csvAnnotation.ingestionParameters
      val csvColumns: List<Pair<FieldName /* = kotlin.String */, ColumnName /* = kotlin.String */>> =
         getColumnsFromType(result.type)

      val target = StringWriter()
      val printer = CsvFormatFactory.fromParameters(parameters).let { format ->
         when {
            parameters.firstRecordAsHeader && result is TypedCollection -> setHeader(csvColumns, format).print(target)
            parameters.firstRecordAsHeader && typedInstanceInfo.index == 0 -> setHeader(csvColumns, format).print(target)
            else -> format.print(target)
         }
      }

      val fieldNamesToWrite = csvColumns.map { it.first }
      convertAndWrite(rawValue, printer, fieldNamesToWrite)
      return target.toString()
   }

   private fun getColumnsFromType(type: Type): List<Pair<FieldName, ColumnName>> {
      val memberType = type.collectionType ?: type
      // Use the fields as it's a list, rather than a map - this makes
      // the ordering consistent
      return (memberType.taxiType as ObjectType).fields
         .filter { field -> field.accessor != null && field.accessor is ColumnAccessor }
         .map { field -> field.name to (field.accessor as ColumnAccessor).path.unquoted() }
   }

   private fun convertAndWrite(rawValue: Any, printer: CSVPrinter, csvColumns: List<FieldName>) {
      when (rawValue) {
         is Iterable<*> -> rawValue
            .filterNotNull()
            .forEach { convertAndWrite(it, printer, csvColumns) }

         is Map<*, *> -> {
            // Grab just the values from the map that have a column associated with them
            val csvColumnValues = csvColumns.map { rawValue[it] }
            printer.printRecord(*csvColumnValues.toTypedArray())
         }
         else -> error("CsvFormat can't handle a raw result of type ${rawValue::class.simpleName}")
      }

   }

   private fun setHeader(csvColumns: List<Pair<FieldName, ColumnName>>, csvFormat: CSVFormat): CSVFormat {
      val columnNames = csvColumns.map { it.second }
      return csvFormat.withHeader(*columnNames.toTypedArray())
         .withSkipHeaderRecord(false)
   }
}

private typealias FieldName = String
private typealias ColumnName = String


fun String.unquoted(): String = replaceFirst("^\"".toRegex(), "").replaceFirst("\"$".toRegex(), "").replace("\\\"", "\"")
