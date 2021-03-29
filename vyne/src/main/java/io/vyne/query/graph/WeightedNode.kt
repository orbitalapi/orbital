package io.vyne.query.graph

import es.usc.citius.hipster.model.impl.WeightedNode
import io.vyne.schemas.Relationship


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

fun WeightedNode<Relationship, Element, Double>.nodeDescription(): String {
   return if (this.previousNode() == null) {
      "Start : ${this.state()}"
   } else {
      // "${edge.vertex1} -[${edge.edgeValue}]-> ${edge.vertex2}"
      "${this.previousNode().state().label()} -[${this.action()}]-> ${this.state().label()} (cost: ${this.cost})"
   }
}
