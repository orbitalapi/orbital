package io.osmosis.polymer

import es.usc.citius.hipster.graph.DirectedEdge
import es.usc.citius.hipster.graph.GraphEdge
import es.usc.citius.hipster.graph.HipsterDirectedGraph
import io.osmosis.polymer.schemas.Relationship
import io.osmosis.polymer.utils.log

/**
 * Builds a trimmed down version of the Polymer graph,
 * suitable for displaying to users.
 */
class DisplayGraphBuilder {
   companion object {
      val VISIBLE_ELEMENTS = setOf(ElementType.OPERATION, ElementType.TYPE, ElementType.MEMBER)

   }
   fun convertToDisplayGraph(graph: HipsterDirectedGraph<Element, Relationship>):HipsterDirectedGraph<Element,Relationship> {
      val viewGraphBuilder = HipsterGraphBuilder.create<Element,Relationship>()

      graph.vertices()
         .filter { visibleInDisplayGraph(it) }
         .forEach { element ->
            graph.outgoingEdgesOf(element)
               .mapNotNull { convertToDisplayGraphRelationship(it,graph) }
               .distinct()
               .forEach { edge -> viewGraphBuilder.connect(edge.vertex1).to(edge.vertex2).withEdge(edge.edgeValue) }
         }

      return viewGraphBuilder.createDirectedGraph()
   }

   private fun convertToDisplayGraphRelationship(edge: GraphEdge<Element, Relationship>, graph: HipsterDirectedGraph<Element, Relationship>): GraphEdge<Element, Relationship>? {


      when (edge.edgeValue) {
         Relationship.IS_ATTRIBUTE_OF -> return null // This is a bi-directional relationship, so keep the HAS_ATTRIBUTE direction
         Relationship.REQUIRES_PARAMETER -> {
            val paramType = edge.vertex2.valueAsQualifiedName().fullyQualifiedName.removePrefix("param/")
            return DirectedEdge(edge.vertex1, type(paramType),Relationship.REQUIRES_PARAMETER)
         }
         Relationship.PROVIDES -> return DirectedEdge(edge.vertex1, type(edge.vertex2.value.toString()), Relationship.PROVIDES)
      }

      if (VISIBLE_ELEMENTS.contains(edge.vertex1.elementType) && VISIBLE_ELEMENTS.contains(edge.vertex2.elementType)) {
         return edge
      }
      log().error("Unhandled graph simplification scenario - we should be making this simpler, but returning node as-is to avoid errors")
      return edge
   }

   private fun visibleInDisplayGraph(element: Element): Boolean {
      return VISIBLE_ELEMENTS.contains(element.elementType)
   }


}
