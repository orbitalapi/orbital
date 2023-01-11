package io.vyne.schemas

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.vyne.VersionedSource
import lang.taxi.expressions.Expression
import lang.taxi.types.FormatsAndZoneOffset

@JsonDeserialize(`as` = DefaultPartialSchema::class)
interface PartialSchema {
   val types: Set<out PartialType>
   val services: Set<PartialService>

   fun type(name: QualifiedName): PartialType {
      return types.single { it.name == name }
   }
}

data class DefaultPartialSchema(
   override val types: Set<out PartialType>,
   override val services: Set<out PartialService>
) : PartialSchema {
   override fun type(name: QualifiedName): PartialType = types.first { it.name == name }

}

@JsonDeserialize(`as` = DefaultPartialType::class)
@JsonIgnoreProperties(ignoreUnknown = true)
interface PartialType {
   val name: QualifiedName
   val attributes: Map<AttributeName, Field>
   val modifiers: List<Modifier>
   val metadata: List<Metadata>

   @get:JsonProperty("inheritsFrom")
   val inheritsFromTypeNames: List<QualifiedName>
   val enumValues: List<EnumValue>

   @get:JsonProperty("typeParameters")
   val typeParametersTypeNames: List<QualifiedName>
   val typeDoc: String?

   val isPrimitive: Boolean
   val isEnum: Boolean
   val isCollection: Boolean
   val isScalar: Boolean

   val fullyQualifiedName: String
   val basePrimitiveTypeName: QualifiedName?
   val format: List<String>?
   val declaresFormat: Boolean

   val unformattedTypeName: QualifiedName?
   val formatAndZoneOffset: FormatsAndZoneOffset?
   val offset: Int?
   val expression: Expression?

   val sources: List<VersionedSource>
}

data class DefaultPartialType(
   override val name: QualifiedName,
   override val attributes: Map<AttributeName, Field>,
   override val modifiers: List<Modifier>,
   override val metadata: List<Metadata>,
   override val inheritsFromTypeNames: List<QualifiedName>,
   override val enumValues: List<EnumValue>,
   override val typeParametersTypeNames: List<QualifiedName>,
   override val typeDoc: String?,

   override val isPrimitive: Boolean,
   override val isEnum: Boolean,
   override val isCollection: Boolean,
   override val isScalar: Boolean,

   override val fullyQualifiedName: String,
   override val basePrimitiveTypeName: QualifiedName?,
   override val format: List<String>?,
   override val declaresFormat: Boolean,

   override val unformattedTypeName: QualifiedName?,
   override val offset: Int?,
   override val expression: Expression?,

   override val sources: List<VersionedSource>
) : PartialType {
   override val formatAndZoneOffset: FormatsAndZoneOffset? = FormatsAndZoneOffset.forNullable(format, offset)
}
