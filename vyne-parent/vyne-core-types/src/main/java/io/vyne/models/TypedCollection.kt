package io.vyne.models

import io.vyne.schemas.Schema
import io.vyne.schemas.Type


data class TypedCollection(override val type: Type, override val value: List<TypedInstance>) : List<TypedInstance> by value, TypedInstance {

   companion object {
      /**
       * Constructs a TypedCollection by interrogating the contents of the
       * provided list.
       * If the list is empty, then an exception is thrown
       */
      fun from(populatedList: List<TypedInstance>): TypedCollection {
         // TODO : Find the most compatiable abstract type.
         val first = populatedList.firstOrNull()
            ?: error("An empty list was passed, where a populated list was expected.  Cannot infer type.")
         return TypedCollection(first.type, populatedList)
      }
   }

   override fun withTypeAlias(typeAlias: Type): TypedInstance {
      return TypedCollection(typeAlias, value)
   }

   override fun valueEquals(valueToCompare: TypedInstance): Boolean {
      if (valueToCompare !is TypedCollection) {
         return false
      }
      if (!this.type.resolvesSameAs(valueToCompare.type)) {
         return false
      }
      if (this.size != valueToCompare.size) {
         return false
      }
      this.forEachIndexed { index, typedInstance ->
         if (!typedInstance.valueEquals(valueToCompare[index])) {
            // Fail as soon as any values don't equal
            return false
         }
      }
      return true
   }

   fun parameterizedType(schema: Schema): Type {
      return schema.type("lang.taxi.Array<${type.name.parameterizedName}>")
   }
}
