package com.orbitalhq.models

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedInstancePredicateProvider
import com.orbitalhq.models.TypedNull
import com.orbitalhq.query.TypedInstanceValidPredicate
import com.orbitalhq.schemas.Field

/**
 * Rejects a value if it is null, empty, or whitespace
 */
object FirstNotEmptyPredicate : TypedInstanceValidPredicate, TypedInstancePredicateProvider {
   override fun isValid(typedInstance: TypedInstance?): Boolean {
      return when {
         typedInstance == null -> false
         typedInstance is TypedNull -> false
         typedInstance.value == null -> false
         typedInstance.value.toString().trim().isEmpty() -> false
         else -> true
      }
   }

   override fun provide(field: Field): TypedInstanceValidPredicate? {
      return if (field.metadata.any { it.name.fullyQualifiedName == "FirstNotEmpty" }) {
         this
      } else {
         null
      }
   }
}
