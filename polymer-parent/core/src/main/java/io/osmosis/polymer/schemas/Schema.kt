package io.osmosis.polymer.schemas

import java.io.Serializable

fun String.fqn(): QualifiedName {
   return QualifiedName(this)
}

data class TypeReference(val name: QualifiedName, val isCollection: Boolean = false)
data class QualifiedName(val fullyQualifiedName: String) : Serializable {
   val name: String
      get() = fullyQualifiedName.split(".").last()

   override fun toString(): String = fullyQualifiedName
}

typealias AttributeName = String
typealias AttributeType = QualifiedName
data class Type(val name: QualifiedName, val attributes: Map<AttributeName, TypeReference> = emptyMap()) {
   constructor(name:String, attributes: Map<AttributeName, TypeReference> = emptyMap()) : this(name.fqn(),attributes)
   val isScalar = attributes.isEmpty()
}


interface Schema {
   val attributes: Set<QualifiedName>
   val types: Set<Type>
   val links: Set<Link>
   fun type(name: String): Type {
      return this.types.firstOrNull { it.name.fullyQualifiedName == name } ?:
         throw IllegalArgumentException("Type $name was not found within this schema")
   }

   fun type(name: QualifiedName): Type {
      return type(name.fullyQualifiedName)
   }

   fun hasType(name: String): Boolean {
      return this.types.any { it.name.fullyQualifiedName == name }
   }
}
