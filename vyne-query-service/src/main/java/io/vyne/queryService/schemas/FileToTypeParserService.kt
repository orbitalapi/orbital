package io.vyne.queryService.schemas

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import feign.Headers
import io.vyne.cask.api.ContentType
import io.vyne.cask.api.CsvIngestionParameters
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.models.csv.CsvImporterUtil
import io.vyne.models.csv.ParsedCsvContent
import io.vyne.models.csv.ParsedTypeInstance
import io.vyne.models.json.isJsonArray
import io.vyne.schemaStore.SchemaProvider
import io.vyne.testcli.commands.TestSpec
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.io.outputstream.ZipOutputStream
import net.lingala.zip4j.model.ZipParameters
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.io.ByteArrayInputStream
import javax.servlet.http.HttpServletResponse

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
                                      @RequestParam("testSpecName") testSpecName: String,
                                      response: HttpServletResponse
   ) {
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
            null to ZipParameters().apply {
               fileNameInZip = directoryName
            },
            rawContent.toByteArray() to ZipParameters().apply {
               fileNameInZip = directoryName + sourceFileName
            },
            generateJsonByteArray(typed) to ZipParameters().apply {
               fileNameInZip = directoryName + expectedFileName
            },
            objectMapper
               .writerWithDefaultPrettyPrinter()
               .writeValueAsBytes(spec) to ZipParameters().apply {
               fileNameInZip = "$directoryName$filenameSafeSpecName.spec.json"
            }
         )

         response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
         response.setHeader(HttpHeaders.CONTENT_DISPOSITION, """attachment; filename="$filenameSafeSpecName.spec.zip" """)


         val zipStream = ZipOutputStream(response.outputStream)
         contentPairs.forEach { (byteArray, zipParameters) ->
            zipStream.putNextEntry(zipParameters)
            if (byteArray != null) {
               // TODO : This could blow up wiht a large file, as we end up with lots in memory.
               // Probably ok for now.
               zipStream.write(byteArray)
            }
            zipStream.closeEntry()
         }
         zipStream.close()
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
}

