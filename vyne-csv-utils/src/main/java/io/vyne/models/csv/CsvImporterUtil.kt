package io.vyne.models.csv

import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObjectFactory
import io.vyne.schemas.Schema
import org.apache.commons.csv.CSVParser

object CsvImporterUtil {

   fun parseCsvToType(rawContent: String,
                      parameters: CsvIngestionParameters,
                      schema: Schema,
                      typeName: String
   ): List<ParsedTypeInstance> {
      val format = CsvFormatFactory.fromParameters(parameters)
      val content = trimContent(rawContent, parameters.ignoreContentBefore)
      val parsed = CSVParser.parse(content, format)
      val targetType = schema.type(typeName)
      val nullValues = parameters.nullValue
      val records = parsed.records
         .filter { parsed.headerNames == null || parsed.headerNames.isEmpty() || parsed.headerNames.size == it.size() }
         .map { csvRecord ->
            ParsedTypeInstance(TypedObjectFactory(targetType, csvRecord, schema, nullValues, source = Provided).build())
         }
      return records
   }

   fun parseCsvToRaw(rawContent: String,
                     parameters: CsvIngestionParameters
   ): ParsedCsvContent {
      val format = CsvFormatFactory.fromParameters(parameters)
      val content = trimContent(rawContent, parameters.ignoreContentBefore)

      val parsed = CSVParser.parse(content, format)
      val records = parsed.records
         .filter { parsed.headerNames == null || parsed.headerNames.isEmpty() || parsed.headerNames.size == it.size() }
         .map { it.toList() }
      val headers = parsed.headerMap?.keys?.toList() ?: emptyList()
      return ParsedCsvContent(headers, records)
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
