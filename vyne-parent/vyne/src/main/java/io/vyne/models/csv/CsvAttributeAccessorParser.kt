package io.vyne.models.csv

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.io.CharSource
import io.vyne.models.PrimitiveParser
import io.vyne.models.TypedInstance
import io.vyne.schemas.Type
import lang.taxi.types.ColumnAccessor
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import java.util.concurrent.TimeUnit

internal object CsvDocumentCacheBuilder {
   fun newCache(): LoadingCache<String, List<CSVRecord>> = CacheBuilder.newBuilder()
      .expireAfterAccess(2, TimeUnit.SECONDS)
      .build(object : CacheLoader<String, List<CSVRecord>>() {
         override fun load(key: String): List<CSVRecord> {
            return CSVFormat.DEFAULT
               .withFirstRecordAsHeader()
               .parse(CharSource.wrap(key).openBufferedStream())
               .records
         }
      })
}

/**
 * Parses a single attribute, defined by a ColumnAccessor
 */
class CsvAttributeAccessorParser(private val primitiveParser: PrimitiveParser = PrimitiveParser(), private val documentCache: LoadingCache<String, List<CSVRecord>> = CsvDocumentCacheBuilder.newCache()) {
   fun parse(content: String, type: Type, accessor: ColumnAccessor): TypedInstance {
      val source = documentCache.get(content)
      val instances = source.map { record -> parseToType(type, accessor, record) }
      if (instances.size == 1) {
         return instances.first()
      } else {
         TODO("Need to work out how to handle a CSV with multiple instances")
      }

   }

   internal fun parseToType(type: Type, accessor: ColumnAccessor, record: CSVRecord): TypedInstance {
      val value = record.get(accessor.index)
      return primitiveParser.parse(value, type)
   }
}

