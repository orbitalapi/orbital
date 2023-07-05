package io.vyne.cockpit.core.schemas.importing.swagger

import io.vyne.cockpit.core.schemas.importing.BaseUrlLoadingSchemaConverter
import io.vyne.cockpit.core.schemas.importing.SchemaConversionRequest
import io.vyne.cockpit.core.schemas.importing.SchemaConverter
import lang.taxi.generators.GeneratedTaxiCode
import lang.taxi.generators.openApi.GeneratorOptions
import lang.taxi.generators.openApi.TaxiGenerator
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

@Component
class SwaggerSchemaConverter(
   webClient: WebClient = defaultWebCLient
) :
   SchemaConverter<SwaggerConverterOptions>, BaseUrlLoadingSchemaConverter(webClient) {
   companion object {
      const val SWAGGER_FORMAT = "swagger"

      const val MAX_SIZE = 1_000_000 // 1MBish
      val defaultWebCLient = WebClient.builder()
         .exchangeStrategies(ExchangeStrategies.builder().codecs { c -> c.defaultCodecs().maxInMemorySize(MAX_SIZE) }.build())
         .build()
   }

   override val conversionParamsType: KClass<SwaggerConverterOptions> = SwaggerConverterOptions::class
   override val supportedFormats = listOf(SWAGGER_FORMAT)
   private val swaggerToTaxiGenerator = TaxiGenerator()

   override fun convert(request: SchemaConversionRequest, options: SwaggerConverterOptions): Mono<GeneratedTaxiCode> {
      return loadSwaggerContents(options)
         .map { swagger ->
            swaggerToTaxiGenerator.generateAsStrings(
               swagger, options.defaultNamespace, GeneratorOptions(
                  options.serviceBasePath
               )
            )
         }

   }

   private fun loadSwaggerContents(options: SwaggerConverterOptions): Mono<String> {
      return when {
         options.swagger != null -> Mono.just(options.swagger)
         options.url != null -> loadSchema(options.url)
         else -> error("Unhandled Swagger config - expected either swagger, or a url to load from")
      }
   }
}

data class SwaggerConverterOptions(
   val defaultNamespace: String,
   val serviceBasePath: String? = null,
   val swagger: String? = null,
   val url: String? = null
)
