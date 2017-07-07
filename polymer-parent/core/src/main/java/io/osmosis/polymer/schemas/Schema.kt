package io.osmosis.polymer.schemas

import java.io.Serializable

fun String.fqn(): QualifiedName {
   return QualifiedName(this)
}

data class QualifiedName(val fullyQualifiedName: String) : Serializable {
   val name: String
      get() = fullyQualifiedName.split(".").last()

   override fun toString(): String = fullyQualifiedName
}

typealias AttributeName = String
typealias AttributeType = QualifiedName
data class Type(val name: QualifiedName, val attributes: Map<AttributeName, AttributeType>)

interface Schema {
   val attributes: Set<QualifiedName>
   val types: Set<Type>
   val links: Set<Link>
}
