package io.vyne.models.csv

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.io.CharSource
import io.vyne.models.DataSource
import io.vyne.models.PrimitiveParser
import io.vyne.models.TypedInstance
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import lang.taxi.types.ColumnAccessor
import lang.taxi.types.ReadFunction
import lang.taxi.types.ReadFunctionFieldAccessor
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import org.apache.commons.lang3.StringUtils
import java.lang.IllegalArgumentException
import java.lang.StringBuilder
import java.util.concurrent.TimeUnit

internal object CsvDocumentCacheBuilder {
   val sharedDocumentCache = newCache()
   fun newCache(): LoadingCache<String, List<CSVRecord>> = CacheBuilder.newBuilder()
      .expireAfterAccess(10, TimeUnit.SECONDS)
      .build(object : CacheLoader<String, List<CSVRecord>>() {
         override fun load(key: String): List<CSVRecord> {
            // Here, we're just trying to see if there's more than
            // 1 line - intimiating that there's a csv header here
            // However, this method is probably very poor performance
            val suspectHeaderExists = key.lines().size > 1
            val format = if (suspectHeaderExists) {
               CSVFormat.DEFAULT.withFirstRecordAsHeader()
            } else {
               CSVFormat.DEFAULT
            }
            return format
               .parse(CharSource.wrap(key).openBufferedStream())
               .records
         }
      })
}

/**
 * Parses a single attribute, defined by a ColumnAccessor
 */
class CsvAttributeAccessorParser(private val primitiveParser: PrimitiveParser = PrimitiveParser(), private val documentCache: LoadingCache<String, List<CSVRecord>> = CsvDocumentCacheBuilder.sharedDocumentCache) {
   fun parse(content: String, type: Type, accessor: ColumnAccessor, schema: Schema, source:DataSource, nullable: Boolean): TypedInstance {
      val csvRecords = documentCache.get(content)
      val instances = csvRecords.map { record -> parseToType(type, accessor, record, schema, source = source, nullable = nullable) }
      if (instances.size == 1) {
         return instances.first()
      } else {
         TODO("Need to work out how to handle a CSV with multiple instances")
      }

   }

   fun parseToType(type: Type, accessor: ColumnAccessor, record: CSVRecord, schema: Schema, nullValues: Set<String> = emptySet(), source: DataSource, nullable: Boolean): TypedInstance {
      val value =
         when {
            accessor.index is Int -> record.get(accessor.index!! as Int - 1)
            accessor.index is String -> record.get((accessor.index!! as String).trim('"'))
            accessor.defaultValue != null -> accessor.defaultValue!!
            else -> throw IllegalArgumentException("Index type must be either Int or String.")
         }

      if (isNull(nullable, nullValues, value)) {
         return TypedInstance.from(type, null, schema, source = source)
      }

      try {
         return primitiveParser.parse(value, type, source)
      } catch (e: Exception) {
         val message = "Failed to parse value $value from column ${accessor.index} to type ${type.name.fullyQualifiedName} - ${e.message}"
         throw ParsingException(message, e)
      }
   }

   fun evaluate(value: Any,
                targetType: Type,
                schema: Schema,
                accessor: ReadFunctionFieldAccessor,
                nullValues: Set<String>,
                source: DataSource,
                nullable: Boolean): TypedInstance {

      return when(accessor.readFunction) {
         ReadFunction.CONCAT -> {
            val arguments = accessor.arguments.mapNotNull { readFunctionArgument ->
               if (readFunctionArgument.columnAccessor != null) {
                  parseColumnData(value, targetType, schema, readFunctionArgument.columnAccessor!!, nullValues, source, nullable).value
               } else {
                  readFunctionArgument.value
               }
            }



            val builder = StringBuilder()
            arguments.forEach { builder.append(it.toString()) }
            TypedInstance.from(targetType, builder.toString(), schema, source = source)
         }

         ReadFunction.LEFTUPPERCASE -> {
            // leftAndUpperCase(column("CCY"), 3)
            val columnAccessor = requireNotNull(accessor.arguments.first().columnAccessor)
            val len = accessor.arguments[1].value as Int
            val columnValue = parseColumnData(value, targetType, schema, columnAccessor, nullValues, source, nullable).value
            if (columnValue == null) {
               TypedInstance.from(targetType, null, schema, source = source)
            } else {
               val leftUpperCaseArg = columnValue.toString()
               TypedInstance.from(targetType, StringUtils.left(leftUpperCaseArg, len), schema, source = source)
            }
         }

         ReadFunction.MIDUPPERCASE -> {

         }

         else -> error("Only concat is allowed")
      }
   }

   companion object {
      fun isNull(nullable: Boolean, nullValues: Set<String>, value: Any?) = (nullable && ((nullValues.isNotEmpty() && nullValues.contains(value)) || (nullValues.isEmpty() && value.toString().isEmpty())))
      fun cellValue(accessor: ColumnAccessor, record: CSVRecord) = when {
         accessor.index is Int -> record.get(accessor.index!! as Int - 1)
         accessor.index is String -> record.get((accessor.index!! as String).trim('"'))
         accessor.defaultValue != null -> accessor.defaultValue!!
         else -> throw IllegalArgumentException("Index type must be either Int or String.")
      }
   }
}

