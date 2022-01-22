package io.vyne.queryService.schemas.importing

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.queryService.schemas.editor.LocalSchemaEditingService
import io.vyne.queryService.schemas.editor.SchemaSubmissionResult
import lang.taxi.generators.GeneratedTaxiCode
import mu.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import kotlin.reflect.KClass

@Component
class CompositeSchemaImporter(
   private val importers: List<SchemaConverter<out Any>>,
   private val schemaEditor: LocalSchemaEditingService,
   private val objectMapper: ObjectMapper
) {
   private val logger = KotlinLogging.logger {}

   init {
      logger.info { "Found ${importers.size} schema converters:  ${importers.joinToString { it::class.simpleName!! }}" }
   }

   fun preview(request: SchemaConversionRequest): Mono<SchemaSubmissionResult> {
      return doConversion(request, validateOnly = true)
   }

   private fun doConversion(request: SchemaConversionRequest, validateOnly: Boolean): Mono<SchemaSubmissionResult> {
      val importer = findImporter(request.format)
      // Only convert if we got something other than what we asked for.
      // In tests, we typically pass the correct type, and trying to run that through a
      // converter can cause problems (eg., using @JsonDeserialize annotations)
      val options = if (request.options::class == importer.conversionParamsType) {
         request.options
      } else {
         objectMapper.convertValue(request.options, importer.conversionParamsType.java)
      }
      val taxi = importer.convert(request, options)

      return taxi.flatMap { generatedCode ->
         schemaEditor.submit(generatedCode.concatenatedSource, validateOnly)
      }

   }

   fun import(request: SchemaConversionRequest): Mono<SchemaSubmissionResult> {
      return doConversion(request, validateOnly = false)
   }

   private fun findImporter(format: String): SchemaConverter<Any> {
      return importers.firstOrNull { it.supportedFormats.contains(format) } as SchemaConverter<Any>?
         ?: error("No imported found for format $format")
   }
}

interface SchemaConverter<TConversionParams : Any> {
   val supportedFormats: List<String>
   val conversionParamsType: KClass<TConversionParams>

   /**
    * Converts a schema to Taxi
    */
   fun convert(request: SchemaConversionRequest, options: TConversionParams): Mono<GeneratedTaxiCode>
}

data class SchemaConversionRequest(
   val format: String,
   val options: Any = emptyMap<String, Any>()
)
