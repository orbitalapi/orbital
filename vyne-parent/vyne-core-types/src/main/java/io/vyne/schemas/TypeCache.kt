package io.vyne.schemas


interface TypeCache {
   fun type(name: String): Type
   fun type(name: QualifiedName): Type
   fun hasType(name: String): Boolean
   fun hasType(name: QualifiedName): Boolean
}
