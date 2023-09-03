package com.orbitalhq.schemas

import com.orbitalhq.query.graph.edges.EvaluatedEdge

/**
 * See SimplifiedSearchPaths for a description of why this is needed
 */
enum class LinkType(val defaultCost: Double) {
   START_POINT(defaultCost = 0.0),
   OBJECT_NAVIGATION(defaultCost = 0.01),
   PARAM_POPULATION(defaultCost = 0.01),
   OPERATION_INVOCATION(defaultCost = 5.0)
}

enum class Relationship(
   val description: String,
   val linkType: LinkType,
   val defaultCost: Double = linkType.defaultCost,

   // Defines how much each edge is penalized for having been evaluated
   // previously, but not provided a result.
   val defaultIncrementalCost:Double = defaultCost
) {
   IS_ATTRIBUTE_OF("Is an attribute of", LinkType.OBJECT_NAVIGATION),
   // If we've used an attribute in a path before, we want to penalize it, to allow other unused attributes to become
   // more viable
   HAS_ATTRIBUTE("Has attribute", LinkType.OBJECT_NAVIGATION, defaultIncrementalCost = 0.5),
   IS_TYPE_OF("Is type of", LinkType.OBJECT_NAVIGATION),

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
   TYPE_PRESENT_AS_ATTRIBUTE_TYPE("Is used as attribute type", LinkType.OBJECT_NAVIGATION),
   INSTANCE_HAS_ATTRIBUTE("Instance has attribute", LinkType.OBJECT_NAVIGATION),
   REQUIRES_PARAMETER("Requires parameter", LinkType.PARAM_POPULATION),
   // We don't penalize Is Parameter On, as we want to try the same value on different services.
   // Penalizing this edge will discourage repeat attempts to use the same value in different places
   IS_PARAMETER_ON("Is parameter on", LinkType.PARAM_POPULATION, defaultIncrementalCost = 0.0),
   CAN_CONSTRUCT_QUERY("Can construct query", LinkType.PARAM_POPULATION),
   IS_INSTANCE_OF("Is instanceOfType of", LinkType.OBJECT_NAVIGATION),
   PROVIDES("provides", LinkType.OPERATION_INVOCATION),
   EXTENDS_TYPE("extends", LinkType.OBJECT_NAVIGATION),
   // We don't penalize Is Can Populate, as we want to try the same value on different services.
   // Penalizing this edge will discourage repeat attempts to use the same value in different places
   CAN_POPULATE("can populate", LinkType.OBJECT_NAVIGATION, defaultIncrementalCost = 0.0),
   IS_SYNONYM_OF("is synonym of", LinkType.OBJECT_NAVIGATION),
   CAN_ARRAY_MAP_TO("can iterate and map to", LinkType.OBJECT_NAVIGATION);

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

fun List<EvaluatedEdge>.description(): String {
   return this.joinToString("\n") { it.description }
}

fun List<Link>.describe(): String {
   return this.joinToString("\n") { it.toString() }
}