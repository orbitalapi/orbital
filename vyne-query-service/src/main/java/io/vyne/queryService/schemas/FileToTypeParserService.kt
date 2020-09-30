package io.vyne.queryService.schemas

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import io.vyne.cask.api.CsvIngestionParameters
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.models.csv.CsvImporterUtil
import io.vyne.models.csv.ParsedCsvContent
import io.vyne.models.csv.ParsedTypeInstance
import io.vyne.models.json.isJsonArray
import io.vyne.schemaStore.SchemaProvider
import io.vyne.utils.xml.XmlDocumentProvider
import org.apache.commons.io.IOUtils
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux

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
            rawContent, parameters)
      } catch (e: Exception) {
         throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message, e)
      }
   }

   @PostMapping("/api/csv/downloadParsed")
   fun parseAndDownload(@RequestBody rawContent: String,
                        @RequestParam("delimiter", required = false, defaultValue = ",") csvDelimiter: Char,
                        @RequestParam("firstRecordAsHeader", required = false, defaultValue = "true") firstRecordAsHeader: Boolean,
                        @RequestParam("ignoreContentBefore", required = false) ignoreContentBefore: String? = null,
                        @RequestParam("containsTrailingDelimiters", required = false, defaultValue = "false") containsTrailingDelimiters: Boolean = false
   ): ByteArray {
      try {
         val parameters = CsvIngestionParameters(
            csvDelimiter, firstRecordAsHeader, ignoreContentBefore = ignoreContentBefore, containsTrailingDelimiters = containsTrailingDelimiters
         )
         return downloadParsedData(CsvImporterUtil.parseCsvToRaw(rawContent, parameters))
      } catch (e: Exception) {
         throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message, e)
      }
   }

   @PostMapping("/api/csv/downloadTypedParsed")
   fun parseToTypeAndDownload(@RequestBody rawContent: String,
                             @RequestParam("delimiter", required = false, defaultValue = ",") csvDelimiter: Char,
                             @RequestParam("type") typeName: String,
                             @RequestParam("firstRecordAsHeader", required = false, defaultValue = "true") firstRecordAsHeader: Boolean,
                             @RequestParam("ignoreContentBefore", required = false) ignoreContentBefore: String? = null,
                             @RequestParam("containsTrailingDelimiters", required = false, defaultValue = "false") containsTrailingDelimiters: Boolean = false
   ): ByteArray {
      try {
         val parameters = CsvIngestionParameters(
            csvDelimiter, firstRecordAsHeader, ignoreContentBefore = ignoreContentBefore, containsTrailingDelimiters = containsTrailingDelimiters
         )
         val typed = CsvImporterUtil.parseCsvToType(
            rawContent,
            parameters,
            schemaProvider.schema(),
            typeName
         );
         return downloadTypedParsedData(typed)
      } catch (e: Exception) {
         throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message, e)
      }
   }

   fun downloadTypedParsedData(parsedContent: List<ParsedTypeInstance>): ByteArray {
      val typedNamedInstanceList = parsedContent.map { it.typeNamedInstance }
      return objectMapper
         .writeValueAsString(typedNamedInstanceList)
         .toByteArray()
   }

   fun downloadParsedData(parsedContent: ParsedCsvContent): ByteArray {

      val records = parsedContent.records.map {
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
   }

   @PostMapping("/api/xml/parse")
   fun parseXmlContentToType(@RequestBody rawContent: String,
                             @RequestParam("type") typeName: String,
                             @RequestParam("elementSelector", required = false) elementSelector: String? = null): List<ParsedTypeInstance> {
      val schema = schemaProvider.schema()
      val targetType = schema.type(typeName)
      try {
         return IOUtils.toInputStream(rawContent).use {
            XmlDocumentProvider(elementSelector)
               .parseXmlStream(Flux.just(it))
               .map { document -> TypedInstance.from(targetType, document, schema, source = Provided) }
               .map { typedInstance -> ParsedTypeInstance(typedInstance)  }
               .collectList()
               .block()
         }
      } catch (e: Exception) {
         throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message, e)
      }
   }
}

