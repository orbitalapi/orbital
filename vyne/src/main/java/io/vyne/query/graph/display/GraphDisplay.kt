package io.vyne.query.graph.display

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import es.usc.citius.hipster.graph.GraphEdge
import es.usc.citius.hipster.graph.HipsterDirectedGraph
import io.vyne.query.graph.Element
import io.vyne.query.graph.ElementType
import io.vyne.schemas.Relationship


data class SchemaGraphNode(val id: String, val label: String, val type: ElementType, val nodeId: String)
data class SchemaGraphLink(val source: String, val target: String, val label: String)
data class SchemaGraph(private val nodeSet: Set<SchemaGraphNode>, private val linkSet: Set<SchemaGraphLink>) {
   val nodes: Map<String, SchemaGraphNode> = nodeSet.associateBy { it.id }
   val links: Map<Int, SchemaGraphLink> = linkSet.associateBy { it.hashCode() }
}

fun HipsterDirectedGraph<*, *>.displayGraph(): SchemaGraph {
   return GraphDisplayUtils.buildDisplayGraph(this as HipsterDirectedGraph<Element, Relationship>)
}

fun HipsterDirectedGraph<*, *>.displayGraphJson(): String {
   return jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this.displayGraph())
}

object GraphDisplayUtils {
   fun buildDisplayGraph(edges: MutableIterable<GraphEdge<Element, Relationship>>): SchemaGraph {
      val schemaGraphNodes = edges.collateElements().map { toSchemaGraphNode(it) }.toSet()
      val schemaGraphLinks = edges.map { toSchemaGraphLink(it) }.toSet()


      return SchemaGraph(schemaGraphNodes, schemaGraphLinks)
   }

   fun buildDisplayGraph(graph: HipsterDirectedGraph<Element, Relationship>): SchemaGraph {
      val nodes = graph.vertices().map { element -> toSchemaGraphNode(element) }.toSet()
      val links = graph.edges().map { edge -> toSchemaGraphLink(edge) }.toSet()
      return SchemaGraph(nodes, links)
   }

   private fun toSchemaGraphLink(edge: GraphEdge<Element, Relationship>) =
      SchemaGraphLink(edge.vertex1.browserSafeId(), edge.vertex2.browserSafeId(), edge.edgeValue.description)

   private fun toSchemaGraphNode(element: Element): SchemaGraphNode {
      return SchemaGraphNode(
         id = element.browserSafeId(),
//         label = element.graphNode().value.toString(),
         label = element.label(),
         type = element.elementType,
         nodeId = element.value.toString().replace("/", ":")
      )
   }

   fun Element.browserSafeId(): String {
      return this.toString()
         .replace(".", "")
         .replace("<", "")
         .replace(">", "")
         .replace("/", "")
         .replace("(", "")
         .replace(")", "")
         .replace("_", "")
         .replace("-", "")
         .replace("@", "")
   }
}

private fun Iterable<GraphEdge<Element, Relationship>>.collateElements(): Set<Element> {
   return this.flatMap { listOf(it.vertex1, it.vertex2) }.toSet()
}
