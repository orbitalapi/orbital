package io.osmosis.polymer.schemas

enum class Relationship(val description: String) {
   IS_ATTRIBUTE_OF("Is an attribute of"),
   HAS_ATTRIBUTE("Has attribute"),
   IS_TYPE_OF("Is type of"),
   REQUIRES_PARAMETER("Requires parameter"),
   IS_PARAMETER_ON("Is parameter on"),
   PROVIDES("provides")
}

data class Link(val start: QualifiedName, val relationship: Relationship, val end: QualifiedName, val cost: Int = 1) {
   override fun toString(): String {
      return "$start -[${relationship.description}]-> $end"
   }
}

data class Path(val start: QualifiedName, val target: QualifiedName, val links: List<Link>) {
   val exists: Boolean = links.isNotEmpty()
   val description: String
      get() = this.links.joinToString(", ")
}
