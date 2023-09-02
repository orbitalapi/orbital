// Have commented this out.
// It depenends on jsonSchema-to-taxi, which depends on an unmaintained project with a CVE of high severity.
// Can pull this back in if someone wants it.

//package com.orbitalhq.cockpit.core.schemas.importing.jsonschema
//
//import com.orbitalhq.cockpit.core.schemas.importing.*
//import lang.taxi.generators.GeneratedTaxiCode
//import lang.taxi.generators.jsonSchema.TaxiGenerator
//import org.everit.json.schema.loader.SchemaLoader
//import org.springframework.stereotype.Component
//import org.springframework.web.reactive.function.client.WebClient
//import reactor.core.publisher.Mono
//import java.net.URL
//import kotlin.reflect.KClass
//
//@Component
//class JsonSchemaConverter(
//   webClient: WebClient = WebClient.create(),
//) :
//   SchemaConverter<JsonSchemaConverterOptions>, BaseUrlLoadingSchemaConverter(webClient) {
//   companion object {
//      const val SUPPORTED_FORMAT = "jsonSchema"
//   }
//
//   override val supportedFormats: List<String> = listOf(SUPPORTED_FORMAT)
//   override val conversionParamsType: KClass<JsonSchemaConverterOptions> = JsonSchemaConverterOptions::class
//
//   override fun convert(
//      request: SchemaConversionRequest,
//      options: JsonSchemaConverterOptions
//   ): Mono<SourcePackageWithMessages> {
//
//      val schemaLoaderBuilder = createSchemaLoaderBuilder(options)
//      val generator = TaxiGenerator(schemaLoader = schemaLoaderBuilder)
//      return Mono.create { sink ->
//         val generatedCode = when {
//            options.url != null -> generator.generateAsStrings(
//               URL(options.url),
//               options.defaultNamespace
//            )
//
//            options.jsonSchema != null -> generator.generateAsStrings(
//               options.jsonSchema,
//               options.defaultNamespace
//            )
//
//            else -> error("Expected either url or jsonSchema to be provided")
//         }
//         val sourcePackage = generatedCode.toSourcePackageWithMessages(
//            request.packageIdentifier,
//            generatedImportedFileName("JsonSchema")
//         )
//         sink.success(sourcePackage)
//      }
//
//   }
//
//   private fun createSchemaLoaderBuilder(options: JsonSchemaConverterOptions): SchemaLoader.SchemaLoaderBuilder {
//      val builder = SchemaLoader.builder()
//         .let { builder ->
//            if (options.resolveUrlsRelativeToUrl != null) {
//               builder.resolutionScope(options.resolveUrlsRelativeToUrl)
//            } else {
//               builder
//            }
//         }.let { builder ->
//            when (options.schemaVersion) {
//               JsonSchemaConverterOptions.SchemaVersion.DRAFT_6 -> builder.draftV6Support()
//               JsonSchemaConverterOptions.SchemaVersion.DRAFT_7 -> builder.draftV7Support()
//               else -> builder
//            }
//         }
//      return builder
//   }
//
//
//}
//
//data class JsonSchemaConverterOptions(
//   val defaultNamespace: String? = null,
//   val jsonSchema: String? = null,
//   val url: String? = null,
//   val resolveUrlsRelativeToUrl: String? = null,
//   val schemaVersion: SchemaVersion = SchemaVersion.INFERRED,
//) {
//   enum class SchemaVersion {
//      INFERRED,
//      DRAFT_6,
//      DRAFT_7
//   }
//}
//
