package io.vyne.models.csv

import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObjectFactory
import io.vyne.schemas.Schema
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser

object CsvImporterUtil {

   fun parseCsvToType(rawContent: String,
                      typeName: String,
                      csvDelimiter: Char = ',',
                      schema: Schema,
                      firstRecordAsHeader: Boolean,
                      nullValue: String? = null,
                      ignoreContentBefore: String? = null
   ): List<ParsedTypeInstance> {
      // TODO : We need to find a better way to pass the metadata of how to parse a CSV into the TypedInstance.parse()
      // method.

      val format = getCsvFormat(csvDelimiter, firstRecordAsHeader)
      val content = trimContent(rawContent, ignoreContentBefore)
      val parsed = CSVParser.parse(content, format)
      val targetType = schema.type(typeName)
      val nullValues = listOfNotNull(nullValue).toSet()
      val records = parsed.records
         .filter { parsed.headerNames == null || parsed.headerNames.isEmpty() || parsed.headerNames.size == it.size() }
         .map { csvRecord -> ParsedTypeInstance(TypedObjectFactory(targetType, csvRecord, schema, nullValues, source = Provided).build())
         }
      return records
   }

   fun parseCsvToRaw(rawContent: String,
                     csvDelimiter: Char,
                     firstRecordAsHeader: Boolean,
                     ignoreContentBefore: String? = null
   ): ParsedCsvContent {
      val format = getCsvFormat(csvDelimiter, firstRecordAsHeader)
      val content = trimContent(rawContent, ignoreContentBefore)

      val parsed = CSVParser.parse(content, format)
      val records = parsed.records
         .filter { parsed.headerNames == null || parsed.headerNames.isEmpty() || parsed.headerNames.size == it.size() }
         .map { it.toList() }
      val headers = parsed.headerMap?.keys?.toList() ?: emptyList()
      return ParsedCsvContent(headers, records)
   }

   private fun getCsvFormat(csvDelimiter: Char, firstRecordAsHeader: Boolean): CSVFormat {
      return CSVFormat
         .DEFAULT
         .withTrailingDelimiter()
         .withIgnoreEmptyLines()
         .withDelimiter(csvDelimiter).let {
            if (firstRecordAsHeader) {
               it.withFirstRecordAsHeader()
                  .withAllowDuplicateHeaderNames()
                  .withAllowMissingColumnNames()
            } else {
               it
            }
         }
   }

   private fun trimContent(content: String, ignoreContentBefore: String?): String {
      return if (ignoreContentBefore != null) {
         val index = content.indexOf(ignoreContentBefore)
         if (index > 0) {
            content.removeRange(0 until index)
         } else {
            content
         }
      } else {
         content
      }
   }
}

data class ParsedCsvContent(val headers: List<String>, val records: List<List<String>>)

data class ParsedTypeInstance(
   val instance: TypedInstance
) {
   val typeNamedInstance = instance.toTypeNamedInstance()
   val raw = instance.toRawObject()
}
