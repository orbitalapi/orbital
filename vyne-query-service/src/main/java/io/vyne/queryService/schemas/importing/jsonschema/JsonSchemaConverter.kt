package io.vyne.queryService.schemas.importing.jsonschema

import io.vyne.queryService.schemas.importing.BaseUrlLoadingSchemaConverter
import io.vyne.queryService.schemas.importing.SchemaConversionRequest
import io.vyne.queryService.schemas.importing.SchemaConverter
import lang.taxi.generators.GeneratedTaxiCode
import lang.taxi.generators.jsonSchema.TaxiGenerator
import org.everit.json.schema.loader.SchemaLoader
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.net.URL
import java.time.Duration
import kotlin.reflect.KClass

@Component
class JsonSchemaConverter(
   webClient: WebClient = WebClient.create(),
   httpClientTimeout: Duration = Duration.ofSeconds(10)
) :
   SchemaConverter<JsonSchemaConverterOptions>, BaseUrlLoadingSchemaConverter(webClient, httpClientTimeout) {
   companion object {
      const val SUPPORTED_FORMAT = "jsonSchema"
   }

   override val supportedFormats: List<String> = listOf(SUPPORTED_FORMAT)
   override val conversionParamsType: KClass<JsonSchemaConverterOptions> = JsonSchemaConverterOptions::class

   override fun convert(
      request: SchemaConversionRequest,
      options: JsonSchemaConverterOptions
   ): GeneratedTaxiCode {

      val schemaLoaderBuilder = createSchemaLoaderBuilder(options)
      val generator = TaxiGenerator(schemaLoader = schemaLoaderBuilder)
      return when {
         options.url != null -> generator.generateAsStrings(
            URL(options.url),
            options.defaultNamespace
         )
         options.jsonSchema != null -> generator.generateAsStrings(
            options.jsonSchema,
            options.defaultNamespace
         )
         else -> error("Expected either url or jsonSchema to be provided")
      }
   }

   private fun createSchemaLoaderBuilder(options: JsonSchemaConverterOptions): SchemaLoader.SchemaLoaderBuilder {
      val builder = SchemaLoader.builder()
         .let { builder ->
            if (options.resolveUrlsRelativeToUrl != null) {
               builder.resolutionScope(options.resolveUrlsRelativeToUrl)
            } else {
               builder
            }
         }.let { builder ->
            when (options.schemaVersion) {
               JsonSchemaConverterOptions.SchemaVersion.DRAFT_6 -> builder.draftV6Support()
               JsonSchemaConverterOptions.SchemaVersion.DRAFT_7 -> builder.draftV7Support()
               else -> builder
            }
         }
      return builder
   }


}

data class JsonSchemaConverterOptions(
   val defaultNamespace: String,
   val jsonSchema: String? = null,
   val url: String? = null,
   val resolveUrlsRelativeToUrl: String? = null,
   val schemaVersion: SchemaVersion = SchemaVersion.INFERRED
) {
   enum class SchemaVersion {
      INFERRED,
      DRAFT_6,
      DRAFT_7
   }
}
