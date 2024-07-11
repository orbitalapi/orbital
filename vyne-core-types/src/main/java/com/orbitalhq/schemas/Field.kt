package com.orbitalhq.schemas

import com.fasterxml.jackson.annotation.JsonIgnore
import com.orbitalhq.utils.ImmutableEquality
import lang.taxi.accessors.Accessor
import lang.taxi.services.operations.constraints.Constraint
import lang.taxi.types.FieldProjection
import lang.taxi.types.FieldSetExpression
import lang.taxi.types.FormatsAndZoneOffset

// Note: I'm progressively moving this towards Taxi schemas, as discussed
// on the Type comment.
data class Field(
   val type: QualifiedName,
   val modifiers: List<FieldModifier>,
   @get:JsonIgnore
   val accessor: Accessor?,
   @get:JsonIgnore
   val readCondition: FieldSetExpression?,
   val typeDoc: String?,
//   @get:JsonIgnore
//   val formula: Formula? = null,
   val nullable: Boolean = false,
   val typeDisplayName: String = type.longDisplayName,
   val metadata: List<Metadata> = emptyList(),
   val sourcedBy: FieldSource? = null,
   @get:JsonIgnore
   val fieldProjection: FieldProjection? = null,
   val format: FormatsAndZoneOffset?,
   // If the field is an anonymous type, store the type here.
   val anonymousType: Type? = null,
   @get:JsonIgnore
   val constraints: List<Constraint> = emptyList()
) {
   init {
      if (anonymousType != null && anonymousType.paramaterizedName != type.parameterizedName) {
         error("Field has been initialized incorrectly - the provided anonymous type does not match the provided name")
      }
   }

   fun resolveType(schema: Schema): Type {
      return anonymousType ?: schema.type(this.type)
   }

   fun hasMetadata(name: QualifiedName): Boolean {
      return this.metadata.any { it.name == name }
   }

   private val cachedHashCode: Int = run {
      var result = type.hashCode()
      result = 31 * result + modifiers.hashCode()
      result = 31 * result + typeDoc.hashCode()
      result = 31 * result + metadata.hashCode()
      result
   }

   override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is Field) return false
      if (other.cachedHashCode !== this.cachedHashCode) return false

      return type == other.type &&
         modifiers == other.modifiers &&
         typeDoc == other.typeDoc &&
         metadata == other.metadata
   }

   override fun hashCode(): Int {
      return cachedHashCode
   }

   fun getMetadata(name: QualifiedName): Metadata {
      return this.metadata.firstOrNull { it.name == name }
         ?: error("No metadata named ${name.longDisplayName} is present on field type ${type.longDisplayName}")
   }
}

enum class FieldModifier {
   CLOSED
}

data class FieldSource(
   val attributeName: AttributeName,
   val attributeType: QualifiedName,
   val sourceType: QualifiedName,
   val attributeAnonymousType: Type? = null
)
