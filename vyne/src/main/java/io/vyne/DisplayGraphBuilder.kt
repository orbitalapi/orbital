package io.vyne

import es.usc.citius.hipster.graph.DirectedEdge
import es.usc.citius.hipster.graph.GraphEdge
import es.usc.citius.hipster.graph.HipsterDirectedGraph
import io.vyne.query.graph.Element
import io.vyne.query.graph.ElementType
import io.vyne.query.graph.GraphConnection
import io.vyne.query.graph.type
import io.vyne.schemas.OperationNames
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Relationship
import io.vyne.utils.log

/**
 * Builds a trimmed down version of the Vyne graph,
 * suitable for displaying to users.
 */
class DisplayGraphBuilder {
   companion object {
      val VISIBLE_ELEMENTS = setOf(ElementType.OPERATION, ElementType.TYPE, ElementType.MEMBER)

   }

   fun convertToDisplayGraph(graph: HipsterDirectedGraph<Element, Relationship>): HipsterDirectedGraph<Element, Relationship> {
      val viewGraphBuilder = HipsterGraphBuilder.create<Element, Relationship>()

      val connections = graph.vertices()
         .filter { visibleInDisplayGraph(it) }
//         .map { fixOperationNames(it) }
         .flatMap { element ->
            graph.outgoingEdgesOf(element)
               .mapNotNull { convertToDisplayGraphRelationship(it, graph) }
               .distinct()
               .map { edge -> GraphConnection(edge.vertex1,edge.vertex2,edge.edgeValue) }
         }

      return viewGraphBuilder.createDirectedGraph(connections)
   }

   private fun toDisplayElement(element: Element): Element {
      return when (element.elementType) {
         ElementType.OPERATION -> fixOperationNames(element)
         // TODO : Improve display of others
         else -> element
      }
   }

   private fun fixOperationNames(element: Element): Element {
      return if (element.elementType == ElementType.OPERATION) {
         val displayName = OperationNames.displayNameFromOperationName(element.valueAsQualifiedName())
          Element(displayName, ElementType.OPERATION)
      } else element
   }

   private fun convertToDisplayGraphRelationship(edge: GraphEdge<Element, Relationship>, graph: HipsterDirectedGraph<Element, Relationship>): GraphEdge<Element, Relationship>? {


      when (edge.edgeValue) {
         Relationship.IS_ATTRIBUTE_OF -> return null // This is a bi-directional relationship, so keep the HAS_ATTRIBUTE direction
         Relationship.REQUIRES_PARAMETER -> {
            val paramType = edge.vertex2.valueAsQualifiedName().fullyQualifiedName.removePrefix("param/")
            return DirectedEdge(edge.vertex1, type(paramType), Relationship.REQUIRES_PARAMETER)
         }
         Relationship.PROVIDES -> return DirectedEdge(edge.vertex1, type(edge.vertex2.value.toString()), Relationship.PROVIDES)
         else -> { }// do nothing
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

private fun Element.mapName(nameMapper: (QualifiedName) -> String): Element {
   val updatedName = nameMapper(this.valueAsQualifiedName())
   return Element(updatedName, this.elementType, this.instanceValue)
}
