package io.vyne.queryService.schemas.editor

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.vyne.VersionedSource
import io.vyne.schemas.*
import lang.taxi.Operator
import lang.taxi.expressions.Expression
import lang.taxi.services.FilterCapability
import lang.taxi.services.QueryOperationCapability
import lang.taxi.services.SimpleQueryCapability
import lang.taxi.types.FormatsAndZoneOffset

/**
 * Not a real schema!
 * Used to submit edits to types and services from the UI, which we then
 * convert into Taxi, and back into a real schema.
 */
class EditedSchema(
   @JsonDeserialize(contentAs = EditedType::class)
   override val types: Set<PartialType> = emptySet(),
   @JsonDeserialize(contentAs = EditedService::class)
   override val services: Set<PartialService> = emptySet()
) : PartialSchema {
   companion object {
      // used for testing
      fun from(schema: Schema): EditedSchema {
         return EditedSchema(schema.types, schema.services)
      }
   }

   override fun type(name: QualifiedName): PartialType {
      return types.firstOrNull { it.name == name }
         ?: error("Type ${name.fullyQualifiedName} is not present in this collection")
   }

   val typeNames = types.map { it.name }.toSet()
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class EditedType(
   override val name: QualifiedName,
   override val attributes: Map<AttributeName, Field>,
   override val modifiers: List<Modifier>,
   override val metadata: List<Metadata>,
   @JsonProperty("inheritsFrom")
   override val inheritsFromTypeNames: List<QualifiedName>,
   override val enumValues: List<EnumValue>,
   @JsonProperty("typeParameters")
   override val typeParametersTypeNames: List<QualifiedName>,
   override val typeDoc: String?,
   override val isPrimitive: Boolean,
   override val isEnum: Boolean,
   override val isCollection: Boolean,
   override val isScalar: Boolean,
   override val fullyQualifiedName: String,
   override val basePrimitiveTypeName: QualifiedName?,
   override val format: List<String>?,
   override val unformattedTypeName: QualifiedName?,
   override val offset: Int?,
   override val expression: Expression?,
   override val declaresFormat: Boolean,
   override val sources: List<VersionedSource>,
   override val formatAndZoneOffset: FormatsAndZoneOffset?
) : PartialType

@JsonIgnoreProperties(ignoreUnknown = true)
data class EditedService(
   override val name: QualifiedName,
   override val operations: List<EditedOperation>,
   override val queryOperations: List<EditedQueryOperation>,
   override val streamOperations: List<StreamOperation>,
   override val tableOperations: List<TableOperation>,
   override val metadata: List<Metadata>,
   override val typeDoc: String?
) : PartialService

@JsonIgnoreProperties(ignoreUnknown = true)
data class EditedOperation(
   override val qualifiedName: QualifiedName,
   override val parameters: List<EditedOperationParameter>,
   override val metadata: List<Metadata>,
   override val typeDoc: String?,
   override val returnTypeName: QualifiedName
) : PartialOperation

@JsonIgnoreProperties(ignoreUnknown = true)
data class EditedOperationParameter(
   override val name: String?,
   override val typeName: QualifiedName,
   override val metadata: List<Metadata>
) : PartialParameter

@JsonIgnoreProperties(ignoreUnknown = true)
data class EditedQueryOperation(
   override val qualifiedName: QualifiedName,
   override val parameters: List<EditedOperationParameter>,
   override val metadata: List<Metadata>,
   override val typeDoc: String?,
   override val returnTypeName: QualifiedName,
   override val grammar: String,

   override val capabilities: List<QueryOperationCapability>,
   override val hasFilterCapability: Boolean,
   override val supportedFilterOperations: List<Operator>
) : PartialQueryOperation

