package io.vyne.query.graph

import es.usc.citius.hipster.model.impl.WeightedNode
import io.vyne.schemas.LinkType
import io.vyne.schemas.Relationship


/**
 * Takes a path, and reduces it down to the most significant elements.
 *
 * A graph may contain multiple different paths which are materially the same.
 *
 * For example:
 *
 * {OPERATION}:MovieService@@findDirector -[provides]-> {TYPE_INSTANCE}:Director
 * {TYPE_INSTANCE}:Director -[Instance has attribute]-> {PROVIDED_INSTANCE_MEMBER}:Director/birthday
 * {PROVIDED_INSTANCE_MEMBER}:Director/birthday -[Is an attribute of]-> {TYPE_INSTANCE}:DateOfBirth
 * {TYPE_INSTANCE}:DateOfBirth -[Is instanceOfType of]-> {TYPE}:DateOfBirth
 *
 * is materially the same as :
 *
 * {OPERATION}:MovieService@@findDirector -[provides]-> {TYPE_INSTANCE}:Director
 * {TYPE_INSTANCE}:Director -[Is instanceOfType of]-> {TYPE}:Director
 * {TYPE}:Director -[Has attribute]-> {MEMBER}:Director/birthday
 * {MEMBER}:Director/birthday -[Is type of]-> {TYPE}:DateOfBirth
 *
 * both can be reduced to "Invoke findDirector, and pick the Director/birthday element from the result".
 *
 * Given the different types of inputs that can exist, preventing these paths from being placed into the graph has proven
 * too complex.  Therefore, we're trying to let the paths get created, but then simplify them down to their material steps.
 *
 * This lets us take two different-but-equivalent paths, and compare them for equality.
 */
fun WeightedNode<Relationship, Element, Double>.simplifyPath(): SimplifiedPath {
   return this.path()
      .mapNotNull { it.simplify() }
}


private fun WeightedNode<Relationship, Element, Double>.simplify(): Pair<LinkType,Any>? {
   return if (this.previousNode() == null) {
      LinkType.START_POINT to this.state().value
   } else when(this.action()) {
      Relationship.IS_ATTRIBUTE_OF -> null
      Relationship.HAS_ATTRIBUTE -> LinkType.OBJECT_NAVIGATION to this.state().value
      Relationship.IS_TYPE_OF -> null
      Relationship.TYPE_PRESENT_AS_ATTRIBUTE_TYPE -> null
      Relationship.INSTANCE_HAS_ATTRIBUTE -> LinkType.OBJECT_NAVIGATION to this.state().value
      Relationship.REQUIRES_PARAMETER -> null
      Relationship.IS_PARAMETER_ON -> null
      Relationship.IS_INSTANCE_OF -> null
      Relationship.PROVIDES -> LinkType.OPERATION_INVOCATION to (this.previousNode().state().value.toString() + " returns " + this.state().value)
      Relationship.EXTENDS_TYPE -> null
      Relationship.CAN_POPULATE -> LinkType.PARAM_POPULATION to this.state().value
      Relationship.CAN_CONSTRUCT_QUERY ->   LinkType.PARAM_POPULATION to this.state().value
      Relationship.IS_SYNONYM_OF -> null
      Relationship.CAN_ARRAY_MAP_TO -> null
   }
}

typealias SimplifiedPath = List<Pair<LinkType, Any>>


