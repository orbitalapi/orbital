package io.vyne.schemas

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vyne.utils.ImmutableEquality
import lang.taxi.accessors.Accessor
import lang.taxi.types.FieldProjection
import lang.taxi.types.FieldSetExpression
import lang.taxi.types.FormatsAndZoneOffset

// Note: I'm progressively moving this towards Taxi schemas, as discussed
// on the Type comment.
data class Field(
   val type: QualifiedName,
   val modifiers: List<FieldModifier>,
   private val constraintProvider: DeferredConstraintProvider = EmptyDeferredConstraintProvider(),
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
   val anonymousType: Type? = null
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

   private val equality = ImmutableEquality(
      this,
      Field::type,
      Field::modifiers,
      Field::typeDoc,
      Field::metadata
   )

   override fun equals(other: Any?): Boolean = equality.isEqualTo(other)
   override fun hashCode(): Int = equality.hash()


   fun getMetadata(name: QualifiedName): Metadata {
      return this.metadata.firstOrNull { it.name == name }
         ?: error("No metadata named ${name.longDisplayName} is present on field type ${type.longDisplayName}")
   }

   // TODO : Why take the provider, and not the constraints?  I have a feeling it's because
   // we parse fields before we parse their underlying types, so constrains may not be
   // fully resolved at construction time.
   @get:JsonIgnore
   @delegate:JsonIgnore
   val constraints: List<Constraint> by lazy { constraintProvider.buildConstraints() }
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
