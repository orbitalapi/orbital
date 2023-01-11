package io.vyne.queryService.schemas

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import io.vyne.cask.api.ContentType
import io.vyne.models.Provided
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.csv.CsvImporterUtil
import io.vyne.models.csv.CsvIngestionParameters
import io.vyne.models.csv.ParsedCsvContent
import io.vyne.models.csv.ParsedTypeInstance
import io.vyne.models.json.isJson
import io.vyne.models.json.isJsonArray
import io.vyne.query.ResultMode
import io.vyne.query.ValueWithTypeName
import io.vyne.queryService.query.QueryResponseFormatter
import io.vyne.queryService.query.QueryService
import io.vyne.schema.api.SchemaProvider
import io.vyne.schemas.Type
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.spring.http.BadRequestException
import io.vyne.testcli.commands.TestSpec
import io.vyne.utils.xml.XmlDocumentProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.apache.commons.io.IOUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID
import java.util.zip.ZipEntry

@RestController
class FileToTypeParserService(
    val schemaProvider: SchemaProvider,
    val objectMapper: ObjectMapper,
    val queryService: QueryService,
    private val queryResponseFormatter: QueryResponseFormatter
) {

   @PostMapping("/api/content/parse")
   fun parseFileToType(
      @RequestBody rawContent: String,
      @RequestParam("type") typeName: String
   ): List<ParsedTypeInstance> {
      val schema = schemaProvider.schema
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

   @PostMapping("/api/contentAndSchema/parse")
   fun parseContentToTypeWithAdditionalSchema(
      @RequestParam("type") typeName: String,
      @RequestBody request: ContentWithSchemaParseRequest
   ):ContentWithSchemaParseResponse {
      val (compositeSchema, typesInTempSchema) = compileTempSchema(request)
      val targetType = compositeSchema.type(typeName)
      val typedInstance = TypedInstance.from(targetType, request.content, compositeSchema)
      val parsedInstances =  when (typedInstance) {
         is TypedCollection -> typedInstance.map { member -> ParsedTypeInstance(member) }
         else -> listOf(ParsedTypeInstance(typedInstance))
      }
      return ContentWithSchemaParseResponse(parsedInstances, typesInTempSchema)
   }

   @Deprecated("Use parseContentToTypeWithAdditionalSchema and put the CSV import specs in metadata on the type")
   @PostMapping("/api/csvAndSchema/parse")
   fun parseCsvToTypeWithAdditionalSchema(
      @RequestBody request: ContentWithSchemaParseRequest,
      @RequestParam("type") typeName: String,
      @RequestParam("delimiter", required = false, defaultValue = ",") csvDelimiter: Char = ',',
      @RequestParam("firstRecordAsHeader", required = false, defaultValue = "true") firstRecordAsHeader: Boolean = true,
      @RequestParam("nullValue", required = false) nullValue: String? = null,
      @RequestParam("ignoreContentBefore", required = false) ignoreContentBefore: String? = null,
      @RequestParam(
         "containsTrailingDelimiters",
         required = false,
         defaultValue = "false"
      ) containsTrailingDelimiters: Boolean = false
   ): ContentWithSchemaParseResponse {
      val parameters = CsvIngestionParameters(
         csvDelimiter,
         firstRecordAsHeader,
         setOf(nullValue).filterNotNull().toSet(),
         ignoreContentBefore,
         containsTrailingDelimiters
      )
      val (parseResult, typesInTempSchema) = parseCsvWithSchemaData(request, parameters, typeName)
      return ContentWithSchemaParseResponse(
        parseResult,
        typesInTempSchema
     )
   }

   @PostMapping("/api/csvAndSchema/project")
   suspend fun projectCsvToTypeWithAdditionalSchema(
      @RequestBody request: ContentWithSchemaParseRequest,
      @RequestParam("type") typeName: String,
      @RequestParam("targetType") targetTypeName: String,
      @RequestParam("delimiter", required = false, defaultValue = ",") csvDelimiter: Char = ',',
      @RequestParam("firstRecordAsHeader", required = false, defaultValue = "true") firstRecordAsHeader: Boolean = true,
      @RequestParam("nullValue", required = false) nullValue: String? = null,
      @RequestParam("ignoreContentBefore", required = false) ignoreContentBefore: String? = null,
      @RequestParam(
         "containsTrailingDelimiters",
         required = false,
         defaultValue = "false"
      ) containsTrailingDelimiters: Boolean = false,
      @RequestParam("clientQueryId", required=false) clientQueryId:String? = UUID.randomUUID().toString()
   ): Flow<Any> {
      val parameters = CsvIngestionParameters(
         csvDelimiter,
         firstRecordAsHeader,
         setOf(nullValue).filterNotNull().toSet(),
         ignoreContentBefore,
         containsTrailingDelimiters
      )
      val (parseResult, typesInTempSchema, schema) = parseCsvWithSchemaData(request, parameters, typeName)
      val collection = TypedCollection.from(parseResult.map { it.instance })
      val queryResult = queryService.doVyneMonitoredWork(schema = schema) { vyne, queryContextEventBroker ->
         vyne.from(collection, eventBroker = queryContextEventBroker, clientQueryId = clientQueryId)
            .build("$targetTypeName[]") // TODO ... that, but better
      }
      val resultStream = queryResponseFormatter.convertToSerializedContent(queryResult, ResultMode.TYPED, contentType = APPLICATION_JSON_VALUE) as Flow<ValueWithTypeName>
      return resultStream.map { value: ValueWithTypeName ->
         if (value.typeName != null) {
            // this is the first record, so include any anonymous types...
            value.copy(anonymousTypes = objectMapper.writeValueAsString(typesInTempSchema))
         } else {
            value
         }
      }
   }

   private fun parseCsvWithSchemaData(
      request: ContentWithSchemaParseRequest,
      parameters: CsvIngestionParameters,
      typeName: String
   ): Triple<List<ParsedTypeInstance>, List<Type>, TaxiSchema> {
      val (compositeSchema, typesInTempSchema) = compileTempSchema(request)

      val parseResult = try {
         CsvImporterUtil.parseCsvToType(
            request.content,
            parameters,
            compositeSchema,
            typeName
         )
      } catch (e:Exception) {
         throw BadRequestException(e.message!!)
      }

      return Triple(parseResult, typesInTempSchema, compositeSchema)
   }

   private fun compileTempSchema(request: ContentWithSchemaParseRequest): Pair<TaxiSchema, List<Type>> {
      val tempSchemaName = UUID.randomUUID().toString()
      val baseSchema = schemaProvider.schema.asTaxiSchema()
      val compiledInputSchema = TaxiSchema.from(
         request.schema,
         sourceName = tempSchemaName,
         importSources = listOf(baseSchema)
      )

      // Using TaxiSchema.from() allows our user-defined schema to reference import types
      // from the base schema.  But the output isn't truly a merge of both.
      // So we need to add the two docs together
      val compositeTaxiDocument = baseSchema.document.merge(compiledInputSchema.document)
      val compositeSchema = TaxiSchema(compositeTaxiDocument, baseSchema.packages + compiledInputSchema.packages)
      val typesInTempSchema = compiledInputSchema.types
         .filter { type -> type.sources.any { source -> source.name == tempSchemaName } }
      return Pair(compositeSchema, typesInTempSchema)
   }

   @PostMapping("/api/csv/parse")
   fun parseCsvToType(
      @RequestBody rawContent: String,
      @RequestParam("type") typeName: String,
      @RequestParam("delimiter", required = false, defaultValue = ",") csvDelimiter: Char,
      @RequestParam("firstRecordAsHeader", required = false, defaultValue = "true") firstRecordAsHeader: Boolean,
      @RequestParam("nullValue", required = false) nullValue: String? = null,
      @RequestParam("ignoreContentBefore", required = false) ignoreContentBefore: String? = null,
      @RequestParam(
         "containsTrailingDelimiters",
         required = false,
         defaultValue = "false"
      ) containsTrailingDelimiters: Boolean = false
   ): List<ParsedTypeInstance> {
      // TODO : We need to find a better way to pass the metadata of how to parse a CSV into the TypedInstance.parse()
      // method.
      val parameters = CsvIngestionParameters(
         csvDelimiter,
         firstRecordAsHeader,
         setOf(nullValue).filterNotNull().toSet(),
         ignoreContentBefore,
         containsTrailingDelimiters
      )


      return CsvImporterUtil.parseCsvToType(
         rawContent,
         parameters,
         schemaProvider.schema,
         typeName
      )
   }

   @PostMapping("/api/csv")
   fun parseCsvToRaw(
      @RequestBody rawContent: String,
      @RequestParam("delimiter", required = false, defaultValue = ",") csvDelimiter: Char,
      @RequestParam("firstRecordAsHeader", required = false, defaultValue = "true") firstRecordAsHeader: Boolean,
      @RequestParam("ignoreContentBefore", required = false) ignoreContentBefore: String? = null,
      @RequestParam(
         "containsTrailingDelimiters",
         required = false,
         defaultValue = "false"
      ) containsTrailingDelimiters: Boolean = false
   ): ParsedCsvContent {
      try {
         val parameters = CsvIngestionParameters(
            csvDelimiter,
            firstRecordAsHeader,
            ignoreContentBefore = ignoreContentBefore,
            containsTrailingDelimiters = containsTrailingDelimiters
         )
         return CsvImporterUtil.parseCsvToRaw(
            rawContent, parameters
         )
      } catch (e: Exception) {
         throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message, e)
      }
   }

   @PostMapping("/api/csv/downloadParsed")
   fun parseAndDownload(
      @RequestBody rawContent: String,
      @RequestParam("delimiter", required = false, defaultValue = ",") csvDelimiter: Char,
      @RequestParam("firstRecordAsHeader", required = false, defaultValue = "true") firstRecordAsHeader: Boolean,
      @RequestParam("ignoreContentBefore", required = false) ignoreContentBefore: String? = null,
      @RequestParam(
         "containsTrailingDelimiters",
         required = false,
         defaultValue = "false"
      ) containsTrailingDelimiters: Boolean = false
   ): ByteArray {
      try {
         val parameters = CsvIngestionParameters(
            csvDelimiter,
            firstRecordAsHeader,
            ignoreContentBefore = ignoreContentBefore,
            containsTrailingDelimiters = containsTrailingDelimiters
         )
         return downloadParsedData(CsvImporterUtil.parseCsvToRaw(rawContent, parameters))
      } catch (e: Exception) {
         throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message, e)
      }
   }

   @PostMapping("/api/csv/downloadTypedParsed")
   fun parseToTypeAndDownload(
      @RequestBody rawContent: String,
      @RequestParam("delimiter", required = false, defaultValue = ",") csvDelimiter: Char,
      @RequestParam("type") typeName: String,
      @RequestParam("firstRecordAsHeader", required = false, defaultValue = "true") firstRecordAsHeader: Boolean,
      @RequestParam("ignoreContentBefore", required = false) ignoreContentBefore: String? = null,
      @RequestParam(
         "containsTrailingDelimiters",
         required = false,
         defaultValue = "false"
      ) containsTrailingDelimiters: Boolean = false
   ): ByteArray {
      try {
         val parameters = CsvIngestionParameters(
            csvDelimiter,
            firstRecordAsHeader,
            ignoreContentBefore = ignoreContentBefore,
            containsTrailingDelimiters = containsTrailingDelimiters
         )
         val typed = CsvImporterUtil.parseCsvToType(
            rawContent,
            parameters,
            schemaProvider.schema,
            typeName
         )
         return generateJsonByteArray(typed)
      } catch (e: Exception) {
         throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message, e)
      }
   }

   @PostMapping("/api/csv/downloadTypedParsedTestSpec")
   fun parseToTypeAndDownloadTestSpec(
      @RequestBody rawContent: String,
      @RequestParam("delimiter", required = false, defaultValue = ",") csvDelimiter: Char,
      @RequestParam("type") typeName: String,
      @RequestParam("firstRecordAsHeader", required = false, defaultValue = "true") firstRecordAsHeader: Boolean,
      @RequestParam("ignoreContentBefore", required = false) ignoreContentBefore: String? = null,
      @RequestParam(
         "containsTrailingDelimiters",
         required = false,
         defaultValue = "false"
      ) containsTrailingDelimiters: Boolean = false,
      @RequestParam("testSpecName") testSpecName: String
   ): StreamingResponseBody {
      try {
         val parameters = CsvIngestionParameters(
            csvDelimiter,
            firstRecordAsHeader,
            ignoreContentBefore = ignoreContentBefore,
            containsTrailingDelimiters = containsTrailingDelimiters
         )
         val schema = schemaProvider.schema
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
                  schemaProvider.schema,
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
   fun parseXmlContentToType(
      @RequestBody rawContent: String,
      @RequestParam("type") typeName: String,
      @RequestParam("elementSelector", required = false) elementSelector: String? = null
   ): Mono<List<ParsedTypeInstance>> {
      val schema = schemaProvider.schema
      val targetType = schema.type(typeName)
      try {
         return IOUtils.toInputStream(rawContent).use {
            XmlDocumentProvider(elementSelector)
               .parseXmlStream(Flux.just(it))
               .map { document -> TypedInstance.from(targetType, document, schema, source = Provided) }
               .map { typedInstance -> ParsedTypeInstance(typedInstance) }
               .collectList()
         }
      } catch (e: Exception) {
         throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message, e)
      }
   }
}

data class ContentWithSchemaParseRequest(
   val content: String,
   val schema: String
)

data class ContentWithSchemaParseResponse(
   val parsedTypedInstances: List<ParsedTypeInstance>,
   val types: List<Type>
)
