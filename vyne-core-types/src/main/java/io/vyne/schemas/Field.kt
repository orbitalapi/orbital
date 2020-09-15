package io.vyne.schemas

import com.fasterxml.jackson.annotation.JsonIgnore
import lang.taxi.types.Accessor
import lang.taxi.types.FieldSetExpression
import lang.taxi.types.Formula

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
   val typeDoc:String?,
   val defaultValue: Any? = null,
   @get:JsonIgnore
   val formula: Formula? = null,
   val nullable: Boolean = false,
   val typeDisplayName:String = type.longDisplayName
) {
   // TODO : Why take the provider, and not the constraints?  I have a feeling it's because
   // we parse fields before we parse their underlying types, so constrains may not be
   // fully resolved at construction time.
   val constraints: List<Constraint> by lazy { constraintProvider.buildConstraints() }
}

enum class FieldModifier {
   CLOSED
}
