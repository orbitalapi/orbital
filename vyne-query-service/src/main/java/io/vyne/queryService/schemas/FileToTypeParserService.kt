package io.vyne.queryService.schemas

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import io.vyne.cask.api.CsvIngestionParameters
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObjectFactory
import io.vyne.models.csv.CsvImporterUtil
import io.vyne.models.csv.ParsedCsvContent
import io.vyne.models.csv.ParsedTypeInstance
import io.vyne.models.json.isJsonArray
import io.vyne.queryService.ExportType
import io.vyne.schemaStore.SchemaProvider
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVPrinter
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.io.StringWriter

@RestController
class FileToTypeParserService(val schemaProvider: SchemaProvider, val objectMapper: ObjectMapper) {

   @PostMapping("/api/content/parse")
   fun parseFileToType(@RequestBody rawContent: String, @RequestParam("type") typeName: String): List<ParsedTypeInstance> {
      val schema = schemaProvider.schema()
      val targetType = schema.type(typeName)
      try {

         if (isJsonArray(rawContent)) {
            val list = objectMapper.readTree(rawContent) as ArrayNode
            return list.map { ParsedTypeInstance(TypedInstance.from(targetType, it, schema, source = Provided)) }
         }
         return listOf(ParsedTypeInstance(TypedInstance.from(targetType, rawContent, schema, source = Provided)))
      } catch (e: Exception) {
         throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message, e)
      }

   }

   @PostMapping("/api/csv/parse")
   fun parseCsvToType(@RequestBody rawContent: String,
                      @RequestParam("type") typeName: String,
                      @RequestParam("delimiter", required = false, defaultValue = ",") csvDelimiter: Char,
                      @RequestParam("firstRecordAsHeader", required = false, defaultValue = "true") firstRecordAsHeader: Boolean,
                      @RequestParam("nullValue", required = false) nullValue: String? = null,
                      @RequestParam("ignoreContentBefore", required = false) ignoreContentBefore: String? = null,
                      @RequestParam("containsTrailingDelimiters", required = false, defaultValue = "false") containsTrailingDelimiters: Boolean = false
   ): List<ParsedTypeInstance> {
      // TODO : We need to find a better way to pass the metadata of how to parse a CSV into the TypedInstance.parse()
      // method.
      val parameters = CsvIngestionParameters(
         csvDelimiter, firstRecordAsHeader, setOf(nullValue).filterNotNull().toSet(), ignoreContentBefore, containsTrailingDelimiters
      )
      return CsvImporterUtil.parseCsvToType(
         rawContent,
         parameters,
         schemaProvider.schema(),
         typeName
      )
   }

   @PostMapping("/api/csv")
   fun parseCsvToRaw(@RequestBody rawContent: String,
                     @RequestParam("delimiter", required = false, defaultValue = ",") csvDelimiter: Char,
                     @RequestParam("firstRecordAsHeader", required = false, defaultValue = "true") firstRecordAsHeader: Boolean,
                     @RequestParam("ignoreContentBefore", required = false) ignoreContentBefore: String? = null,
                     @RequestParam("containsTrailingDelimiters", required = false, defaultValue = "false") containsTrailingDelimiters: Boolean = false
   ): ParsedCsvContent {
      try {
         val parameters = CsvIngestionParameters(
            csvDelimiter, firstRecordAsHeader, ignoreContentBefore = ignoreContentBefore, containsTrailingDelimiters = containsTrailingDelimiters
         )
         return CsvImporterUtil.parseCsvToRaw(
            rawContent, parameters
         )
      } catch (e: Exception) {
         throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message, e)
      }
   }

   @PostMapping("/api/downloadParsedData/{type}")
   fun downloadParsedData(@RequestBody parsedContent: ParsedCsvContent, @PathVariable("type") exportType: ExportType): ByteArray {
      if (exportType == ExportType.JSON) {
         val records = parsedContent.records.map{
            parsedContent.headers.mapIndexed { index, header ->
               if (index < it.size) {
                  Pair(header, it[index])
               } else {
                  Pair(header, null)
               }
            }.toMap()
         }

         return objectMapper
            .writeValueAsString(records)
            .toByteArray()
      } else {
         val writer = StringWriter()
         val printer = CSVPrinter(writer, CSVFormat.DEFAULT.withFirstRecordAsHeader())
         printer.printRecord(parsedContent.headers)
         parsedContent.records.forEach { printer.printRecord(it) }
         return writer.toString().toByteArray()
      }
   }
}

