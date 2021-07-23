package io.vyne.query.graph

import es.usc.citius.hipster.model.impl.WeightedNode
import io.vyne.schemas.LinkType
import io.vyne.schemas.Relationship
import io.vyne.schemas.Relationship.CAN_POPULATE
import io.vyne.schemas.Relationship.EXTENDS_TYPE
import io.vyne.schemas.Relationship.HAS_ATTRIBUTE
import io.vyne.schemas.Relationship.INSTANCE_HAS_ATTRIBUTE
import io.vyne.schemas.Relationship.IS_ATTRIBUTE_OF
import io.vyne.schemas.Relationship.IS_INSTANCE_OF
import io.vyne.schemas.Relationship.IS_PARAMETER_ON
import io.vyne.schemas.Relationship.IS_SYNONYM_OF
import io.vyne.schemas.Relationship.IS_TYPE_OF
import io.vyne.schemas.Relationship.PROVIDES
import io.vyne.schemas.Relationship.REQUIRES_PARAMETER
import io.vyne.schemas.Relationship.TYPE_PRESENT_AS_ATTRIBUTE_TYPE


fun WeightedNode<Relationship, Element, Double>.hashExcludingWeight(): Int {
   return setOf(this.action()?.hashCode() ?: 0, this.state()?.hashCode() ?: 0).hashCode()
}

fun WeightedNode<Relationship, Element, Double>.pathHashExcludingWeights(): Int {
   return this.path().map { it.hashExcludingWeight() }.hashCode()
}

fun WeightedNode<Relationship, Element, Double>.pathDescription(): String {
   return this.path()
      .joinToString("\n") { it.nodeDescription() }
}

fun List<Pair<LinkType, Any>>.describePath():String {
   return this.joinToString("\n", prefix = "Simplified path ${this.hashCode()}:\n") { it.first.name + " -> "+ it.second.toString() }
}

fun WeightedNode<Relationship, Element, Double>.nodeDescription(): String {
   return if (this.previousNode() == null) {
      "Start : ${this.state()}"
   } else {
      // "${edge.vertex1} -[${edge.edgeValue}]-> ${edge.vertex2}"
      "${this.previousNode().state().label()} -[${this.action()}]-> ${this.state().label()} (cost: ${this.cost})"
   }
}
