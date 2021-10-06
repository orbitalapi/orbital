package io.vyne.queryService

import es.usc.citius.hipster.graph.GraphEdge
import io.vyne.VyneCacheConfiguration
import io.vyne.query.graph.Algorithms
import io.vyne.query.graph.Dataset
import io.vyne.query.graph.Element
import io.vyne.query.graph.ElementType
import io.vyne.query.graph.OperationQueryResult
import io.vyne.query.graph.OperationQueryResultItem
import io.vyne.query.graph.OperationQueryResultItemRole
import io.vyne.query.graph.VyneGraphBuilder
import io.vyne.query.graph.asElement
import io.vyne.query.graph.operation
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.OperationNames
import io.vyne.schemas.Relationship
import io.vyne.schemas.Schema
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class SchemaGraphNode(val id: String, val label: String, val type: ElementType, val nodeId: String)
data class SchemaGraphLink(val source: String, val target: String, val label: String)
data class SchemaGraph(private val nodeSet: Set<SchemaGraphNode>, private val linkSet: Set<SchemaGraphLink>) {
   val nodes: Map<String, SchemaGraphNode> = nodeSet.associateBy { it.id }
   val links: Map<Int, SchemaGraphLink> = linkSet.associateBy { it.hashCode() }

   fun merge(nodes: Set<SchemaGraphNode>, links: Set<SchemaGraphLink>): SchemaGraph {
      return SchemaGraph(
         this.nodeSet + nodes, this.linkSet + links
      )
   }

   fun merge(other: SchemaGraph): SchemaGraph {
      return SchemaGraph(this.nodeSet + other.nodeSet, this.linkSet + other.linkSet)
   }

   companion object {
      fun empty(): SchemaGraph = SchemaGraph(emptySet(), emptySet())
   }
}

@RestController
class TaxiGraphService(
   private val schemaProvider: SchemaProvider,
   private val vyneCacheConfiguration: VyneCacheConfiguration,
   private val typeLineageService: TypeLineageService
) {


   @PostMapping("/api/schemas/taxi-graph")
   fun submitSchema(@RequestBody taxiDef: String): SchemaGraph {

      val schema: TaxiSchema = TaxiSchema.from(taxiDef)
      val graph = VyneGraphBuilder(schema, vyneCacheConfiguration.vyneGraphBuilderCache).build()
      val nodes = graph.vertices().map { toSchemaGraphNode(it) }.toSet()
      val links = graph.edges().map { toSchemaGraphLink(it) }.toSet()
      return SchemaGraph(nodes, links)
   }


   @RequestMapping(value = ["/api/nodes/{elementType}/{nodeName}/links"])
   fun getLinksFromNode(
      @PathVariable("elementType") elementType: ElementType,
      @PathVariable("nodeName") nodeName: String
   ): SchemaGraph {
      val escapedNodeName = nodeName.replace(":", "/")
      val schema = schemaProvider.schema()
      val graph = VyneGraphBuilder(schema, vyneCacheConfiguration.vyneGraphBuilderCache).buildDisplayGraph()
      val element = Element(escapedNodeName, elementType)
      val edges = graph.edgesOf(element)
      return schemaGraph(edges, schema)
   }

   @RequestMapping(value = ["/api/types/{typeName}/links"])
   fun getLinksFromType(@PathVariable("typeName") typeName: String): SchemaGraph {

      val schema: Schema = schemaProvider.schema()
      val graph = VyneGraphBuilder(schema, vyneCacheConfiguration.vyneGraphBuilderCache).buildDisplayGraph()
      val typeElement = if (typeName.contains("@@")) {
         val nodeId = OperationNames.displayNameFromOperationName(typeName.fqn())
         operation(nodeId)
      } else {
         schema.type(typeName).asElement()
      }
//      val typeElement = schema.type(typeName).asElement()
      val edges = graph.edgesOf(typeElement)
      return schemaGraph(edges, schema)
   }

   @RequestMapping(value = ["/api/datasources"])
   fun getImmediateDataSources() =
      Algorithms.getImmediatelyDiscoverableTypes(schemaProvider.schema()).map { it.fullyQualifiedName }

   @RequestMapping(value = ["/api/paths/datasources"])
   fun getImmediatePathsFromDataSources(): List<Dataset> {
      val schema: Schema = schemaProvider.schema()
      return Algorithms.immediateDataSourcePaths(schema)
   }

   @RequestMapping(value = ["/api/datasources/{typeName}"])
   fun getImmediatePathsFromDataSourcesForType(@PathVariable("typeName") typeName: String): List<Dataset> {
      val schema: Schema = schemaProvider.schema()
      return Algorithms.immediateDataSourcePathsFor(schema, typeName)
   }

   @RequestMapping(value = ["/api/types/annotation/{annotation}"])
   fun getTypesWithAnnotation(@PathVariable("annotation") annotation: String): List<String> {
      val schema: Schema = schemaProvider.schema()
      return Algorithms.findAllTypesWithAnnotation(schema, annotation)
   }

   @RequestMapping(value = ["/api/types/operations/{typeName}"])
   fun findAllFunctionsWithArgumentOrReturnValueForType(@PathVariable("typeName") typeName: String): OperationQueryResult {
      val schema: Schema = schemaProvider.schema()
      val graphSearchResult =  Algorithms.findAllFunctionsWithArgumentOrReturnValueForType(schema, typeName)
      // Find the services that have declared they consume this type via another service.
      // We can't display opreation data here, but we can display service data
      val lineageSearchResult = typeLineageService.getLineageForType(typeName)
         .filter { it.consumesVia.isNotEmpty() }
         .map { serviceLineageForType ->
            val serviceThatConsumesType = serviceLineageForType.serviceName
            OperationQueryResultItem(serviceThatConsumesType, null, null, OperationQueryResultItemRole.Input)
         }
      return graphSearchResult.copy(results = graphSearchResult.results + lineageSearchResult)
   }

   @RequestMapping(value = ["/api/types/annotation/operations/{annotation}"])
   fun findAllFunctionsWithArgumentOrReturnValueForAnnotation(@PathVariable("annotation") annotation: String): List<OperationQueryResult> {
      val schema: Schema = schemaProvider.schema()
      return Algorithms.findAllFunctionsWithArgumentOrReturnValueForAnnotation(schema, annotation)
   }


   private fun schemaGraph(edges: MutableIterable<GraphEdge<Element, Relationship>>, schema: Schema): SchemaGraph {
      val schemaGraphNodes = edges.collateElements().map { toSchemaGraphNode(it) }.toSet()
      val schemaGraphLinks = edges.map { toSchemaGraphLink(it) }.toSet()


      return SchemaGraph(schemaGraphNodes, schemaGraphLinks)
   }

   @RequestMapping(value = ["/api/graph"], method = [RequestMethod.GET])
   fun getGraph(
      @RequestParam("startingFrom", required = false) startNode: String?,
      @RequestParam("distance", required = false) distance: Int?
   ): SchemaGraph {

      val schema: Schema = schemaProvider.schema()
      val graph = VyneGraphBuilder(schema, vyneCacheConfiguration.vyneGraphBuilderCache).build()
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
         .toBrowserSafeGraphId()
   }

}

fun String.toBrowserSafeGraphId(): String {
   return this
      .replace(".", "")
      .replace("/", "")
      .replace("(", "")
      .replace(")", "")
      .replace("_", "")
      .replace("-", "")
      .replace("@", "")
      .replace("$", "")
      .replace("<", "")
      .replace(">", "")
}

private fun Iterable<GraphEdge<Element, Relationship>>.collateElements(): Set<Element> {
   return this.flatMap { listOf(it.vertex1, it.vertex2) }.toSet()
}
