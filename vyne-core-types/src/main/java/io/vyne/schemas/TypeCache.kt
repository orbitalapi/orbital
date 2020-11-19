package io.vyne.schemas

import io.vyne.models.TypedInstance


interface TypeCache {
   fun type(name: String): Type
   fun type(name: QualifiedName): Type
   fun hasType(name: String): Boolean
   fun hasType(name: QualifiedName): Boolean
   fun defaultValues(name: QualifiedName): Map<AttributeName, TypedInstance>?
   fun registerAnonymousType(anonymousType: Type)
   fun anonymousTypes(): Set<Type>
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
      error("This is an empty cache")
   }

   override fun registerAnonymousType(anonymousType: Type) {
      TODO("Not yet implemented")
   }

   override fun anonymousTypes(): Set<Type> {
      return setOf()
   }

}
