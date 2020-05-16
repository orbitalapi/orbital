package io.vyne.schemas


interface TypeCache {
   fun type(name: String): Type
   fun type(name: QualifiedName): Type
   fun hasType(name: String): Boolean
   fun hasType(name: QualifiedName): Boolean
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

}
