package com.orbitalhq.formats.csv

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.io.CharSource
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.FailedParsingSource
import com.orbitalhq.models.PrimitiveParser
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.utils.log
import lang.taxi.accessors.ColumnAccessor
import lang.taxi.types.FormatsAndZoneOffset
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
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
   fun parse(content: String, type: Type, accessor: ColumnAccessor, schema: Schema, source:DataSource, nullable: Boolean, format: FormatsAndZoneOffset?): TypedInstance {
      val csvRecords = documentCache.get(content)
      val instances = csvRecords.map { record -> parseToType(
         type,
         accessor,
         record,
         schema,
         source = source,
         nullable = nullable,
         format = format
      ) }
      if (instances.isEmpty()) {
         return TypedNull.create(type)
      }
      if (instances.size == 1) {
         return instances.first()
      } else {
         TODO("Need to work out how to handle a CSV with multiple instances")
      }

   }

   fun parseToType(
      type: Type,
      accessor: ColumnAccessor,
      record: CSVRecord,
      schema: Schema,
      nullValues: Set<String> = emptySet(),
      source: DataSource,
      nullable: Boolean,
      format: FormatsAndZoneOffset?
   ): TypedInstance {
      val value =
         when {
            accessor.index is Int -> record.get(accessor.index!! as Int - 1)
            accessor.index is String -> {
               val columnName = (accessor.index as String).removeSurrounding("\"").removeSurrounding("'")
               if (record.isMapped(columnName)) {
                  record.get(columnName)
               } else {
                  null
               }
            }
            else -> throw IllegalArgumentException("Index type must be either Int or String.")
         }

      if (isNull(value, nullValues)) {
         return TypedInstance.from(type, null, schema, source = source)
      }

      return try {
         primitiveParser.parse(value!!, type, source, format = format)
      } catch (e: Exception) {
         val message = "Failed to parse value $value from column ${accessor.index} to type ${type.name.fullyQualifiedName} - ${e.message}"
         if (nullable) {
            log().warn("Failed to parse the $value for Type ${type.name.shortDisplayName}, since the field is nullable setting its value to null!")
            TypedNull.create(type,  source = FailedParsingSource(value!!, message))
         } else {
            throw ParsingException(message, e)
         }
      }
   }

   private fun isNull(
      value: Any?,
      nullValues: Set<String>
   ): Boolean {
      return value == null ||
         ((nullValues.isNotEmpty() && nullValues.contains(value)) ||
            (nullValues.isEmpty() && value.toString().isEmpty()))
   }
}

