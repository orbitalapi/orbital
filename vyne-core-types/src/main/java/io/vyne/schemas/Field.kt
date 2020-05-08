package io.vyne.schemas

import lang.taxi.types.Accessor
import lang.taxi.types.FieldSetCondition

// Note: I'm progressively moving this towards Taxi schemas, as discussed
// on the Type comment.
data class Field(
   val type: TypeReference,
   val modifiers: List<FieldModifier>,
   private val constraintProvider: DeferredConstraintProvider = EmptyDeferredConstraintProvider(),
   val accessor: Accessor?,
   val readCondition: FieldSetCondition?,
   val typeDoc:String?
) {
   // TODO : Why take the provider, and not the constraints?  I have a feeling it's because
   // we parse fields before we parse their underlying types, so constrains may not be
   // fully resolved at construction time.
   val constraints: List<Constraint> by lazy { constraintProvider.buildConstraints() }
}

enum class FieldModifier {
   CLOSED
}
