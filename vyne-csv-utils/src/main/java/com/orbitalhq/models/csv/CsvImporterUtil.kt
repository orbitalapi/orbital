package com.orbitalhq.models.csv

import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObjectFactory
import com.orbitalhq.models.functions.FunctionRegistry
import com.orbitalhq.schemas.Schema
import org.apache.commons.csv.CSVParser

object CsvImporterUtil {

   fun parseCsvToType(rawContent: String,
                      parameters: CsvIngestionParameters,
                      schema: Schema,
                      typeName: String,
                      functionRegistry: FunctionRegistry = FunctionRegistry.default
   ): List<ParsedTypeInstance> {

      val format = CsvFormatFactory.fromParameters(parameters.withGuessedRecordSeparator(rawContent))

      val content = trimContent(rawContent, parameters.ignoreContentBefore)
      // This is very odd, but finding in debugging that sometimes
      // when using parsed.records, we get an empty list,
      // but CSVParser.parse(content,format).records returns a populated list.
      // Speicifcally happens when parsing a tab delimited file with \n as the line delimiter.
      // Can't explain it.  Possibly a stream issue?
      // Anyway...right now we're parsing twice.  Not great.
      val parsed = CSVParser.parse(content, format)
      val parsedRecords = CSVParser.parse(content, format).records
      val targetType = schema.type(typeName)
      val nullValues = parameters.nullValue
      val records = parsedRecords
//         .filter { parsed.headerNames == null || parsed.headerNames.isEmpty() || parsed.headerNames.size == it.size() }
         .map { csvRecord ->
            ParsedTypeInstance(
               TypedObjectFactory(
                  targetType,
                  csvRecord,
                  schema,
                  nullValues,
                  source = Provided,
                  functionRegistry = functionRegistry,
                  formatSpecs = emptyList()
               ).build()
            )
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

   internal fun trimContent(content: String, ignoreContentBefore: String?): String {
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
