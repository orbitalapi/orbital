package io.vyne.queryService.schemas

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import io.vyne.cask.api.ContentType
import io.vyne.cask.api.CsvIngestionParameters
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.models.csv.CsvImporterUtil
import io.vyne.models.csv.ParsedCsvContent
import io.vyne.models.csv.ParsedTypeInstance
import io.vyne.models.json.isJson
import io.vyne.models.json.isJsonArray
import io.vyne.schemaStore.SchemaProvider
import io.vyne.testcli.commands.TestSpec
import io.vyne.utils.xml.XmlDocumentProvider
import org.apache.commons.io.IOUtils
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.zip.ZipEntry

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
         )
         return generateJsonByteArray(typed)
      } catch (e: Exception) {
         throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message, e)
      }
   }

   @PostMapping("/api/csv/downloadTypedParsedTestSpec")
   fun parseToTypeAndDownloadTestSpec(@RequestBody rawContent: String,
                                      @RequestParam("delimiter", required = false, defaultValue = ",") csvDelimiter: Char,
                                      @RequestParam("type") typeName: String,
                                      @RequestParam("firstRecordAsHeader", required = false, defaultValue = "true") firstRecordAsHeader: Boolean,
                                      @RequestParam("ignoreContentBefore", required = false) ignoreContentBefore: String? = null,
                                      @RequestParam("containsTrailingDelimiters", required = false, defaultValue = "false") containsTrailingDelimiters: Boolean = false,
                                      @RequestParam("testSpecName") testSpecName: String): StreamingResponseBody {
      try {
         val parameters = CsvIngestionParameters(
            csvDelimiter, firstRecordAsHeader, ignoreContentBefore = ignoreContentBefore, containsTrailingDelimiters = containsTrailingDelimiters
         )
         val schema = schemaProvider.schema()
         val targetType = schema.type(typeName)

         val typed = when {
            isJsonArray(rawContent) -> {
               val list = objectMapper.readTree(rawContent) as ArrayNode
               list.map { ParsedTypeInstance(TypedInstance.from(targetType, it, schema, source = Provided)) }
            }
            isJson(rawContent) -> {
               listOf(ParsedTypeInstance(TypedInstance.from(targetType, rawContent, schema, source = Provided)))
            }
            else -> {
               CsvImporterUtil.parseCsvToType(
                  rawContent,
                  parameters,
                  schemaProvider.schema(),
                  typeName
               )
            }
         }

         val filenameSafeSpecName = testSpecName.replace(" ", "-")
         val sourceFileName = "$filenameSafeSpecName-input.csv"
         val expectedFileName = "$filenameSafeSpecName-expected.json"

         val spec = TestSpec(
            testSpecName,
            typeName,
            sourceFileName,
            expectedFileName,
            ContentType.csv,
            parameters
         )

         val directoryName = "$filenameSafeSpecName/"
         val contentPairs = listOf(
            null to ZipEntry(directoryName),
            rawContent.toByteArray() to ZipEntry(directoryName + sourceFileName),
            generateJsonByteArray(typed) to ZipEntry(directoryName + expectedFileName),
            objectMapper
               .writerWithDefaultPrettyPrinter()
               .writeValueAsBytes(spec) to ZipEntry("$directoryName$filenameSafeSpecName.spec.json")
         )

         return StreamingResponseBody { outputStream ->
            val zipStream = java.util.zip.ZipOutputStream(outputStream)
            contentPairs.forEach { (byteArray, zipParameters) ->
               zipStream.putNextEntry(zipParameters)
               if (byteArray != null) {
                  zipStream.write(byteArray)
               }
               zipStream.closeEntry()
            }
            zipStream.close()
         }
      } catch (e: Exception) {
         throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message, e)
      }
   }

   private fun generateJsonByteArray(parsedContent: List<ParsedTypeInstance>): ByteArray {
      val typedNamedInstanceList = parsedContent.map { it.typeNamedInstance }
      return objectMapper
         .writerWithDefaultPrettyPrinter()
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
                             @RequestParam("elementSelector", required = false) elementSelector: String? = null): Mono<List<ParsedTypeInstance>> {
      val schema = schemaProvider.schema()
      val targetType = schema.type(typeName)
      try {
         return IOUtils.toInputStream(rawContent).use {
            XmlDocumentProvider(elementSelector)
               .parseXmlStream(Flux.just(it))
               .map { document -> TypedInstance.from(targetType, document, schema, source = Provided) }
               .map { typedInstance -> ParsedTypeInstance(typedInstance)  }
               .collectList()
         }
      } catch (e: Exception) {
         throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message, e)
      }
   }
}

