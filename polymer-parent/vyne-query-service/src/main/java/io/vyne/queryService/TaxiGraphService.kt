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

data class SchemaGraphNode(val id: String, val label: String, val type: ElementType, val nodeId: String)
data class SchemaGraphLink(val source: String, val target: String, val label: String)
data class SchemaGraph(private val nodeSet: Set<SchemaGraphNode>, private val linkSet: Set<SchemaGraphLink>) {
   val nodes: Map<String, SchemaGraphNode> = nodeSet.associateBy { it.id }
   val links: Map<Int, SchemaGraphLink> = linkSet.associateBy { it.hashCode() }
}

@RestController
class TaxiGraphService(private val schemaProvider: SchemaSourceProvider) {


   @PostMapping("/schemas/taxi-graph")
   @RequestMapping(method = arrayOf(RequestMethod.POST))
   fun submitSchema(@RequestBody taxiDef: String): SchemaGraph {

      val schema: TaxiSchema = TaxiSchema.from(taxiDef)
      val graph = PolymerGraphBuilder(schema).build()
      val nodes = graph.vertices().map { toSchemaGraphNode(it) }.toSet()
      val links = graph.edges().map { toSchemaGraphLink(it) }.toSet()
      return SchemaGraph(nodes, links)
   }


   @RequestMapping(value = "/nodes/{elementType}/{nodeName}/links")
   fun getLinksFromNode(@PathVariable("elementType") elementType: ElementType, @PathVariable("nodeName") nodeName: String): SchemaGraph {
      val escapedNodeName = nodeName.replace(":", "/")
      val schema: TaxiSchema = TaxiSchema.from(schemaProvider.schemaString())
      val graph = PolymerGraphBuilder(schema).buildDisplayGraph()
      val element = Element(escapedNodeName, elementType)
      val edges = graph.edgesOf(element)
      return schemaGraph(edges, schema)
   }

   @RequestMapping(value = "/types/{typeName}/links")
   fun getLinksFromType(@PathVariable("typeName") typeName: String): SchemaGraph {
      val schema: TaxiSchema = TaxiSchema.from(schemaProvider.schemaString())
      val graph = PolymerGraphBuilder(schema).buildDisplayGraph()
      val typeElement = schema.type(typeName).asElement()
      val edges = graph.edgesOf(typeElement)
      return schemaGraph(edges, schema)
   }

   private fun schemaGraph(edges: MutableIterable<GraphEdge<Element, Relationship>>, schema: TaxiSchema): SchemaGraph {
      val schemaGraphNodes = edges.collateElements().map { toSchemaGraphNode(it) }.toSet()
      val schemaGraphLinks = edges.map { toSchemaGraphLink(it) }.toSet()


      return SchemaGraph(schemaGraphNodes, schemaGraphLinks)
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

   private fun toSchemaGraphNode(element: Element): SchemaGraphNode {
      return SchemaGraphNode(id = element.browserSafeId(), label = element.graphNode().value.toString(),
         type = element.elementType,
         nodeId = element.value.toString().replace("/", ":"))
   }

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
