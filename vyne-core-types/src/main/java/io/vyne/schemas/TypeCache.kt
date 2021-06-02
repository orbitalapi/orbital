package io.vyne.schemas

import io.vyne.models.EnumValueKind
import io.vyne.models.TypedEnumValue
import io.vyne.models.TypedInstance
import io.vyne.models.TypedValue


interface TypeCache {
   fun type(name: String): Type
   fun type(name: QualifiedName): Type
   fun hasType(name: String): Boolean
   fun hasType(name: QualifiedName): Boolean
   fun defaultValues(name: QualifiedName): Map<AttributeName, TypedInstance>?
   fun registerAnonymousType(anonymousType: Type)
   fun anonymousTypes(): Set<Type>
   fun enumSynonymsAsTypedValues(typedEnumValue: TypedEnumValue, valueKind: EnumValueKind): List<TypedValue>
   fun enumSynonyms(typedEnumValue: TypedEnumValue):List<TypedEnumValue>
   fun isAssignable(typeA: Type, typeB: Type, considerTypeParameters: Boolean, func:(Type,Type,Boolean) -> Boolean): Boolean
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

   override fun registerAnonymousType(anonymousType: Type) {
      TODO("Not yet implemented")
   }

   override fun anonymousTypes(): Set<Type> {
      return setOf()
   }

   override fun enumSynonymsAsTypedValues(typedEnumValue: TypedEnumValue, valueKind: EnumValueKind): List<TypedValue> = emptyList()
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
