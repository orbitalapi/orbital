package io.vyne.queryService.schemas

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObjectFactory
import io.vyne.models.json.isJsonArray
import io.vyne.schemaStore.SchemaProvider
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class FileToTypeParserService(val schemaProvider: SchemaProvider, val objectMapper: ObjectMapper) {

   @PostMapping("/api/content/parse")
   fun parseFileToType(@RequestBody rawContent: String, @RequestParam("type") typeName: String): List<ParsedTypeInstance>  {
      val schema = schemaProvider.schema()
      val targetType = schema.type(typeName)
      try {

         if(isJsonArray(rawContent)) {
            val list = objectMapper.readTree(rawContent) as ArrayNode
            return list.map {  ParsedTypeInstance(TypedInstance.from(targetType, it, schema, source = Provided)) }
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
                      @RequestParam("nullValue", required = false) nullValue: String? = null
   ): List<ParsedTypeInstance> {
      // TODO : We need to find a better way to pass the metadata of how to parse a CSV into the TypedInstance.parse()
      // method.

      val format = getCsvFormat(csvDelimiter, firstRecordAsHeader)
      val parsed = CSVParser.parse(rawContent, format)
      val schema = schemaProvider.schema()
      val targetType = schema.type(typeName)
      val nullValues = listOfNotNull(nullValue).toSet()
      val records = parsed.records
         .filter { parsed.headerNames == null || parsed.headerNames.size == it.size() }
         .map { csvRecord -> ParsedTypeInstance(TypedObjectFactory(targetType, csvRecord, schema, nullValues, source = Provided).build())
      }
      return records
   }

   @PostMapping("/api/csv")
   fun parseCsvToRaw(@RequestBody rawContent: String,
                     @RequestParam("delimiter", required = false, defaultValue = ",") csvDelimiter: Char,
                     @RequestParam("firstRecordAsHeader", required = false, defaultValue = "true") firstRecordAsHeader: Boolean
   ): ParsedCsvContent {
      val format = getCsvFormat(csvDelimiter, firstRecordAsHeader)

      try {
         val parsed = CSVParser.parse(rawContent, format)
         val records = parsed.records.map { it.toList() }
         val headers = parsed.headerMap?.keys?.toList() ?: emptyList()
         return ParsedCsvContent(headers, records)
      } catch (e: Exception) {
         throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message, e)
      }

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
}

data class ParsedCsvContent(val headers: List<String>, val records: List<List<String>>)

data class ParsedTypeInstance(
   val instance: TypedInstance
) {
   val typeNamedInstance = instance.toTypeNamedInstance()
   val raw = instance.toRawObject()
}
