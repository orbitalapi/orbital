package io.vyne.models.csv

import io.vyne.models.TypeNamedInstance
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.format.ModelFormatSerializer
import io.vyne.models.format.TypedInstanceInfo
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Metadata
import io.vyne.schemas.Type
import lang.taxi.accessors.ColumnAccessor
import lang.taxi.types.ObjectType
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.StringWriter

object CsvFormatSerializer : ModelFormatSerializer {
   override fun write(result: TypedInstance, metadata: Metadata, typedInstanceInfo: TypedInstanceInfo): Any? {
      val csvAnnotation = CsvFormatSpecAnnotation.from(metadata)
      return write(result,csvAnnotation, typedInstanceInfo)
   }

   override fun write(
      result: TypeNamedInstance,
      attributes: Set<AttributeName>,
      metadata: Metadata,
      typedInstanceInfo: TypedInstanceInfo): Any? {
      val csvAnnotation = CsvFormatSpecAnnotation.from(metadata)
      val parameters = csvAnnotation.ingestionParameters
      val rawValue = result.convertToRaw() as? Map<String, Any> ?: return null
      val target = StringWriter()
      val printer = CsvFormatFactory.fromParameters(parameters).let { format ->
         when {
            parameters.firstRecordAsHeader && typedInstanceInfo.index == 0 -> format.withHeader(*attributes.toTypedArray())
               .withSkipHeaderRecord(false).print(target)
            else -> format.print(target)
         }
      }

      convertAndWrite(rawValue, printer, attributes.toList())
      return target.toString()

   }

   override fun write(result: TypeNamedInstance, type: Type, metadata: Metadata, typedInstanceInfo: TypedInstanceInfo): Any? {
      return write(result, type, result.convertToRaw(), CsvFormatSpecAnnotation.from(metadata), typedInstanceInfo)
   }

   fun write(result: TypedInstance, csvAnnotation: CsvFormatSpecAnnotation, typedInstanceInfo: TypedInstanceInfo): Any? {
      return write(result, result.type, result.toRawObject(), csvAnnotation, typedInstanceInfo)
   }

   fun <T> write(result: T, type: Type, rawValue: Any?, csvAnnotation: CsvFormatSpecAnnotation, typedInstanceInfo: TypedInstanceInfo): Any? {
      if (rawValue == null) {
         return null
      }

      val parameters = csvAnnotation.ingestionParameters
      val csvColumns: List<Pair<FieldName /* = kotlin.String */, ColumnName /* = kotlin.String */>> = if (csvAnnotation.useFieldNamesAsColumnNames) {
         lookupColumnsFromFields(type)
      } else {
         lookupColumnsFromAccessors(type)
      }

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

   private fun lookupColumnsFromFields(type: Type): List<Pair<FieldName, ColumnName>> {
      val memberType = type.collectionType ?: type
      // Use the fields as it's a list, rather than a map - this makes
      // the ordering consistent
      return (memberType.taxiType as ObjectType).fields
         .map { field -> field.name to field.name }
   }

   private fun lookupColumnsFromAccessors(type: Type): List<Pair<FieldName, ColumnName>> {
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
