package io.osmosis.polymer.schemas

import io.osmosis.polymer.query.graph.EvaluatedEdge

enum class Relationship(val description: String) {
   IS_ATTRIBUTE_OF("Is an attribute of"),
   HAS_ATTRIBUTE("Has attribute"),
   IS_TYPE_OF("Is type of"),

   // TODO : I keep flip-flopping on why I need this.
   // This is needed, as it allows us to look at a type, and work out
   // where it's used - which opens additional paths.
   // Eg: SIC Code is present on the Client.  It's also an input on
   // a CreditRiskCostRequest.  Without this link, we don't discover
   // how data we know is usable.
   // BUT: It's also problematic, as when looking for a solution, we can
   // navigate this relationship in scnearios assuming we have an instance of
   // something, but don't.
   // Eg: Money -[IsUsedAsAnAttributeType]-> CreditCostRequest/invoiceValue
   // Not very helpful in the middle of a path, as it's not something we can
   // navigate, as we don't have an instance of a CreditCostRequest.
   //
   // NEW PLAN!!!
   // This approach fails because we need to know if we have an instance
   // of a type to be able to evaluate the attributes.
   // So, change the services to connect to instance() elements,
   // and make the instance() elements have the attribute present relationship
   // TODO : Document why this is problematic.
   TYPE_PRESENT_AS_ATTRIBUTE_TYPE("Is used as attribute type"),
   INSTANCE_HAS_ATTRIBUTE("Instance has attribute"),
   REQUIRES_PARAMETER("Requires parameter"),
   IS_PARAMETER_ON("Is parameter on"),
   IS_INSTANCE_OF("Is instanceOfType of"),
   PROVIDES("provides"),
   CAN_POPULATE("can populate");

   override fun toString(): String {
      return this.description
   }
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

   override fun toString(): String = description
}

fun List<EvaluatedEdge>.description():String {
   return this.joinToString("\n") { it.description() }
}

fun List<Link>.describe():String {
   return this.joinToString("\n") { it.toString() }
}
