package io.vyne.queryService

import es.usc.citius.hipster.graph.GraphEdge
import io.osmosis.polymer.Element
import io.osmosis.polymer.ElementType
import io.osmosis.polymer.PolymerGraphBuilder
import io.osmosis.polymer.asElement
import io.osmosis.polymer.schemas.Relationship
import io.osmosis.polymer.schemas.taxi.TaxiSchema
import io.polymer.schemaStore.SchemaSourceProvider
import org.springframework.web.bind.annotation.*

data class SchemaGraphNode(val id: String, val label: String, val type: ElementType)
data class SchemaGraphLink(val source: String, val target: String, val label: String)
data class SchemaGraph(val nodes: Set<SchemaGraphNode>, val links: Set<SchemaGraphLink>)

@RestController
class TaxiGraphService(private val schemaProvider: SchemaSourceProvider) {

   @RequestMapping(value = "/types/{typeName}/links")
   fun getLinks(@PathVariable("typeName") typeName: String): SchemaGraph {
      val schema: TaxiSchema = TaxiSchema.from(schemaProvider.schemaString())
      val graph = PolymerGraphBuilder(schema).build()
      val typeElement = schema.type(typeName).asElement()
      val edges = graph.edgesOf(typeElement)
      val schemaGraphNodes = edges.collateElements().map { toSchemaGraphNode(it) }.toSet()
      val schemaGraphLinks = edges.map { toSchemaGraphLink(it) }.toSet()

      return SchemaGraph(schemaGraphNodes,schemaGraphLinks)
   }

   @RequestMapping(value = "/graph", method = arrayOf(RequestMethod.GET))
   fun getGraph(@RequestParam("startingFrom", required = false) startNode: String?, @RequestParam("distance", required = false) distance: Int?): SchemaGraph {

      val schema: TaxiSchema = TaxiSchema.from(schemaProvider.schemaString())
      val graph = PolymerGraphBuilder(schema).build()
      val nodes = graph.vertices().map { element -> toSchemaGraphNode(element) }.toSet()
      val links = graph.edges().map { edge -> toSchemaGraphLink(edge) }.toSet()
      return SchemaGraph(nodes, links)
   }

   private fun toSchemaGraphLink(edge: GraphEdge<Element, Relationship>) =
      SchemaGraphLink(edge.vertex1.browserSafeId(), edge.vertex2.browserSafeId(), edge.edgeValue.description)

   private fun toSchemaGraphNode(element: Element) =
      SchemaGraphNode(id = element.browserSafeId(), label = element.toString(), type = element.elementType)

   fun Element.browserSafeId(): String {
      return this.toString()
         .replace(".", "")
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
