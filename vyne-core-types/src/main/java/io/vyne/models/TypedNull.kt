package io.vyne.models

import io.vyne.schemas.Type
import lang.taxi.Equality


data class TypedNull(override val type: Type, override val source: DataSource = UndefinedSource) : TypedInstance {
   private val equality = Equality(this, TypedNull::type)
   override fun equals(other: Any?): Boolean = equality.isEqualTo(other)
   override fun hashCode(): Int = equality.hash()
   override val value: Any? = null
   override fun withTypeAlias(typeAlias: Type): TypedInstance {
      return TypedNull(typeAlias)
   }

   override fun valueEquals(valueToCompare: TypedInstance): Boolean {
      return valueToCompare.value == null
   }
}
