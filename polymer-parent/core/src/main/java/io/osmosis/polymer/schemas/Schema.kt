package io.osmosis.polymer.schemas

fun String.fqn(): QualifiedName {
   return QualifiedName(this)
}

data class QualifiedName(val value: String) {
   val name: String
      get() = value.split(".").last()

   override fun toString(): String = value
}

data class Type(val name: QualifiedName, val attributes: List<QualifiedName>)

interface Schema {
   val attributes: Set<QualifiedName>
   val types: Set<Type>
   val links: Set<Link>
}
