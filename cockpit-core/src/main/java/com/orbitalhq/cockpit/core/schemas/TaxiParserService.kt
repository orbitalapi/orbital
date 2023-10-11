package com.orbitalhq.cockpit.core.schemas

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.FirstEntryMetadataResultSerializer
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.spring.http.BadRequestException
import lang.taxi.CompilationException
import lang.taxi.CompilationMessage
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class TaxiParserService(private val schemaProvider: SchemaProvider) {

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
                TypedInstance.from(modelType, modelParseRequest.model, compositeSchema)
            val (typeNamedInstance, rawObject) = if (modelParseRequest.includeTypeInformation) {
                FirstEntryMetadataResultSerializer(
                    anonymousTypes.toSet(), null, QueryOptions.default()
                ).serialize(typedInstance) to null
            } else {
                null to typedInstance.toRawObject()
            }
            ModelParseResult(
                typeNamedInstance,
                rawObject,
                emptyList()
            )
        } catch (e: Exception) {
            ModelParseResult(
                null, null, listOf(e.message ?: e.toString())
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
    val typeNamedInstance: Any?, // a TypeNamedInstance.  If includeTypeInformation == true
    val raw: Any?, // For sending as JSON.  If includeTypeInformation == false Either Map<String,Any> or List<Map<String,Any>>
    val parseErrors: List<String>,
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
