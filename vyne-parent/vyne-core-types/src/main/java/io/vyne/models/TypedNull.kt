package io.vyne.models

import io.vyne.schemas.Type


data class TypedNull(override val type: Type) : TypedInstance {
   override val value: Any? = null
   override fun withTypeAlias(typeAlias: Type): TypedInstance {
      return TypedNull(typeAlias)
   }

   override fun valueEquals(valueToCompare: TypedInstance): Boolean {
      return valueToCompare.value == null
   }
}
