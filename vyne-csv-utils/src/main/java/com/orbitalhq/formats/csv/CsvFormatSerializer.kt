package com.orbitalhq.formats.csv

import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.format.ModelFormatSerializer
import com.orbitalhq.schemas.Metadata
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.accessors.ColumnAccessor
import lang.taxi.types.ObjectType
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.StringWriter

object CsvFormatSerializer : ModelFormatSerializer {
   override fun write(result: TypedInstance, metadata: Metadata, schema: Schema, index: Int): Any? {
      val csvAnnotation = CsvFormatSpecAnnotation.from(metadata)
      return write(result, result.type, result.toRawObject(), csvAnnotation, index)
   }

   override fun write(rawValue: Any?, metadata: Metadata, index: Int): Any? {
      fun getColumnNamesFromMap(map: Map<*, *>): List<Pair<FieldName, ColumnName>> {
         return (map.keys as Collection<String>).map { it to it }
      }

      val columnNames = when {
         rawValue is Map<*, *> -> getColumnNamesFromMap(rawValue)
         rawValue is List<*> && rawValue.firstOrNull() is Map<*, *> -> getColumnNamesFromMap(rawValue.first() as Map<*, *>)
         else -> error("Exporting of raw value is not supported for this value")
      }
      val formatSpec = CsvFormatSpecAnnotation.from(metadata)
      val (stringWriter, csvPrinter) = buildCsvWriter(formatSpec.ingestionParameters, rawValue, columnNames, index)
      convertAndWrite(rawValue, csvPrinter, columnNames.map { it.first })
      return stringWriter.toString()
   }

   override fun writeAsBytes(result: TypedInstance, metadata: Metadata, schema: Schema, index: Int): ByteArray {
      val csv = write(result, metadata, schema, index) as String?
      return csv?.toByteArray() ?: ByteArray(0)
   }

   fun write(result: TypedInstance, csvAnnotation: CsvFormatSpecAnnotation, index: Int): Any? {
      return write(result, result.type, result.toRawObject(), csvAnnotation, index)
   }

   fun <T : Any> write(
      result: T,
      type: Type,
      rawValue: Any?,
      csvAnnotation: CsvFormatSpecAnnotation,
      index: Int
   ): Any? {
      if (rawValue == null) {
         return null
      }

      val parameters = csvAnnotation.ingestionParameters
      val csvColumns: List<Pair<FieldName /* = kotlin.String */, ColumnName /* = kotlin.String */>> =
         if (csvAnnotation.useFieldNamesAsColumnNames) {
            lookupColumnsFromFields(type)
         } else {
            lookupColumnsFromAccessors(type)
         }

      val (target, printer) = buildCsvWriter(parameters, result, csvColumns, index)

      val fieldNamesToWrite = csvColumns.map { it.first }
      convertAndWrite(rawValue, printer, fieldNamesToWrite)
      return target.toString()
   }

   private fun buildCsvWriter(
      parameters: CsvIngestionParameters,
      result: Any,
      csvColumns: List<Pair<FieldName, ColumnName>>,
      index: Int
   ): Pair<StringWriter, CSVPrinter> {
      val target = StringWriter()
      val printer = CsvFormatFactory.fromParameters(parameters).let { format ->
         when {
            parameters.firstRecordAsHeader && result is Collection<*> -> setHeader(csvColumns, format).print(target)
            parameters.firstRecordAsHeader && index == 0 -> setHeader(csvColumns, format).print(target)
            else -> format.print(target)
         }
      }
      return Pair(target, printer)
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


fun String.unquoted(): String =
   replaceFirst("^\"".toRegex(), "").replaceFirst("\"$".toRegex(), "").replace("\\\"", "\"")
