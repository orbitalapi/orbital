package com.orbitalhq.cockpit.core.schemas.parser

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.spring.http.BadRequestException
import com.orbitalhq.spring.query.formats.FormatSpecRegistry
import lang.taxi.CompilationException
import lang.taxi.CompilationMessage
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class TaxiParserService(private val schemaProvider: SchemaProvider,
                        private val formatSpecRegistry: FormatSpecRegistry = FormatSpecRegistry.default()) {

   @PostMapping("/api/taxi/parseModel")
   fun parse(@RequestBody request: TaxiParseRequest): TaxiParseResult {

      val baseSchema = schemaProvider.schema
      val compilationErrors = mutableListOf<CompilationMessage>()
      val parserErrors = mutableListOf<String>()
      val compositeSchema = try {
         if (request.taxi != null) {
            TaxiSchema.fromStrings(
               listOf(request.taxi),
               listOf(baseSchema.asTaxiSchema()),
               onErrorBehaviour = TaxiSchema.Companion.TaxiSchemaErrorBehaviour.THROW_EXCEPTION
            )
         } else {
            baseSchema.asTaxiSchema()
         }
      } catch (e: CompilationException) {
         compilationErrors.addAll(e.errors)
         null
      }
      val newTypes = compositeSchema?.types?.filter { !baseSchema.hasType(it.fullyQualifiedName) } ?: emptyList()

      val modelParseResult = if (compositeSchema != null && request.model != null) {
         parseObject(compositeSchema, request.model, newTypes)
      } else null



      return TaxiParseResult(
         newTypes,
         compilationErrors,
         modelParseResult
      )

   }

   private fun parseObject(
      compositeSchema: TaxiSchema,
      modelParseRequest: ModelParseRequest,
      anonymousTypes: List<Type>
   ): ModelParseResult {
      return try {
         if (!compositeSchema.hasType(modelParseRequest.targetType)) {
            throw BadRequestException("Type ${modelParseRequest.targetType} was not found")
         }
         val modelType = compositeSchema.type(modelParseRequest.targetType)
         val typedInstance =
            TypedInstance.from(modelType, modelParseRequest.model, compositeSchema, formatSpecs = formatSpecRegistry.formats)

         val (json, typeHints) = TypedInstanceInlayHintProvider().generateHints(typedInstance)
         ModelParseResult(
            typedInstance.toRawObject(),
            typeHints,
            emptyList(),
            json
         )
      } catch (e: Exception) {
         ModelParseResult(
            null, emptyList(), listOf(e.message ?: e.toString()), ""
         )
      }
   }
}

/**
 * A request to parse a combination of taxi source (applied in addition to the current schema),
 * and a model.
 *
 * Used from the UI in the Model designer
 */
data class TaxiParseRequest(
   val model: ModelParseRequest? = null,
   /**
    * Additional taxi to append to the existing schema.
    * If null, the existing schema is used as-is.
    */
   val taxi: String? = null,

   )

data class ModelParseRequest(
   val model: String,
   val targetType: String,
   val includeTypeInformation: Boolean
)

data class ModelParseResult(
   val raw: Any?, // For sending as JSON.  If includeTypeInformation == false Either Map<String,Any> or List<Map<String,Any>>
   val typeHints: List<TypePosition>,
   val parseErrors: List<String>,
   val json: String,
)

data class TaxiParseResult(
   val newTypes: List<Type>,
   val compilationErrors: List<CompilationMessage>,
   val parseResult: ModelParseResult?
) {
   val hasParseErrors = parseResult?.parseErrors?.isNotEmpty() ?: false
   val hasCompilationErrors = compilationErrors.isNotEmpty()
   val hasErrors = hasCompilationErrors || hasParseErrors;

}
