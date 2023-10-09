package com.orbitalhq.schemas

import com.orbitalhq.models.TypedEnumValue
import com.orbitalhq.models.TypedInstance


interface TypeCache {
   fun type(name: String): Type
   fun type(name: QualifiedName): Type
   fun hasType(name: String): Boolean
   fun hasType(name: QualifiedName): Boolean
   fun defaultValues(name: QualifiedName): Map<AttributeName, TypedInstance>?
   fun enumSynonyms(typedEnumValue: TypedEnumValue):List<TypedEnumValue>
   fun isAssignable(typeA: Type, typeB: Type, considerTypeParameters: Boolean, func:(Type,Type,Boolean) -> Boolean): Boolean

   fun copy():TypeCache
   fun add(type: Type): Type
}

object EmptyTypeCache : TypeCache {
   override fun type(name: String): Type {
      error("This is an empty cache")
   }

   override fun type(name: QualifiedName): Type {
      error("This is an empty cache")
   }

   override fun hasType(name: String): Boolean = false
   override fun hasType(name: QualifiedName): Boolean = false
   override fun defaultValues(name: QualifiedName): Map<AttributeName, TypedInstance>? {
     return emptyMap()
   }

   override fun add(type: Type): Type {
      TODO("Not yet implemented")
   }

   override fun copy(): TypeCache = this


   override fun enumSynonyms(typedEnumValue: TypedEnumValue): List<TypedEnumValue> = emptyList()

   override fun isAssignable(
      typeA: Type,
      typeB: Type,
      considerTypeParameters: Boolean,
      func: (Type, Type, Boolean) -> Boolean
   ): Boolean {
      return func(typeA,typeB,considerTypeParameters)
   }


}
