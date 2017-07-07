package io.osmosis.polymer

import com.orientechnologies.orient.core.sql.OCommandSQL
import com.tinkerpop.blueprints.Direction
import com.tinkerpop.blueprints.Edge
import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.blueprints.impls.orient.OrientGraph
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory
import com.tinkerpop.blueprints.impls.orient.OrientVertex
import io.osmosis.polymer.GraphAttributes.NODE_TYPE
import io.osmosis.polymer.GraphAttributes.QUALIFIED_NAME
import io.osmosis.polymer.models.json.JsonModel
import io.osmosis.polymer.schemas.*
import io.osmosis.polymer.utils.log

object GraphAttributes {
   val NODE_TYPE = "nodeType"
   val QUALIFIED_NAME = "qualifiedName"
}

enum class NodeTypes {
   ATTRIBUTE,
   TYPE,
   OBJECT
}

data class QueryResult(val result: Any)

class QueryContext(private val polymer: Polymer) {
   fun find(query: String): QueryResult {
      TODO("Not implemented")
   }
}

class Polymer(schemas: List<Schema>, private val graph: OrientGraph) {
   private val schemas = mutableListOf<Schema>()

   constructor() : this(emptyList(), OrientGraphFactory("memory:polymer").setupPool(1, 100).tx)

   fun addData(model: Model): Polymer {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   fun addSchema(schema: Schema): Polymer {
      schemas.add(schema)
      appendToGraph(schema)
      return this
   }

   private fun appendToGraph(schema: Schema) {
//      val attributes = appendAttributes(schema)
      val types = appendTypes(schema)

//      val nodes: Map<QualifiedName, Vertex> = types + attributes
//      schema.links.forEach { link ->
//         graph.addEdge(null, nodes[link.start], nodes[link.end], link.relationship.name)
//         log().debug("Defined link $link")
      //         graph.addEdge(null, )
//      }

   }

   private fun appendTypes(schema: Schema): Map<QualifiedName, Vertex> {
      schema.types.map { type: Type ->
         val typeFullyQualifiedName = type.name.fullyQualifiedName
         val typeNode = addVertex(type.name, NodeTypes.OBJECT)
         type.attributes.map { (attributeName, attributeType) ->
            val attributeQualifiedName = "$typeFullyQualifiedName/$attributeName"
            val (attributeVertex, _) = addVertex(attributeQualifiedName, NodeTypes.ATTRIBUTE).linkTo(typeNode, Relationship.IS_ATTRIBUTE_OF)
            typeNode.linkTo(attributeVertex, Relationship.HAS_ATTRIBUTE)
            addVertex(attributeType, NodeTypes.TYPE).linkFrom(attributeVertex, Relationship.IS_TYPE_OF)
         }
         log().debug("Added attribute ${type.name} to graph")
      }
      // TODO : This should return something meaningful
      return emptyMap()
   }

   private fun addVertex(name: QualifiedName, type: NodeTypes): Vertex {
      return addVertex(name.fullyQualifiedName, type)
   }

   private fun addVertex(name: String, type: NodeTypes): Vertex {
      // TODO : This seems heavyweight.  Might be fine, since it's all in-mem, but
      // consider optimising
      val existing = graph.getVertices(QUALIFIED_NAME, name).toList()
      if (existing.isEmpty()) {
         val vertex = graph.addVertex(name, mapOf(
            QUALIFIED_NAME to name,
            NODE_TYPE to type
         ))

         log().debug("Added vertex of $name with type $type")
         return vertex
      } else {
         return existing.first()
      }

   }

   private fun Vertex.linkTo(inVertex: Vertex, relationship: Relationship): Pair<Vertex, Edge> {
      val edge = graph.addEdge(null, this, inVertex, relationship.name)
      log().debug("Added edge from ${this[QUALIFIED_NAME]} -[${relationship.name}]-> ${inVertex[QUALIFIED_NAME]}  (${this.id} -> ${inVertex.id})")
      return this to edge
   }

   private fun Vertex.linkFrom(outVertex: Vertex, relationship: Relationship): Pair<Vertex, Edge> {
      val edge = graph.addEdge(null, outVertex, this, relationship.name)
      log().debug("Added edge from ${outVertex[QUALIFIED_NAME]} -[${relationship.name}]-> ${this[QUALIFIED_NAME]} (${outVertex.id} -> ${this.id})")
      return this to edge
   }

   private operator fun Vertex.get(propertyName: String): String {
      return this.getProperty(propertyName)
   }

   private fun appendAttributes(schema: Schema): Map<QualifiedName, Vertex> {
      return schema.attributes.map { qualifiedName ->
         val pair = qualifiedName to graph.addVertex(qualifiedName,
            mapOf(
               QUALIFIED_NAME to qualifiedName.fullyQualifiedName,
               NODE_TYPE to NodeTypes.ATTRIBUTE)
         )
         log().debug("Added attribute $qualifiedName to graph")
         pair
      }.toMap()
   }

   fun resolve(query: String): QueryResult {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   fun from(s: JsonModel): QueryContext {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   fun findPath(start: String, target: String): Path {
      return findPath(start.fqn(), target.fqn())
   }

   fun findPath(start: QualifiedName, target: QualifiedName): Path {

      // TODO : This is very hacky
      val startNode = graph.getVertices(QUALIFIED_NAME, start.fullyQualifiedName).toList().firstOrNull() ?: throw IllegalArgumentException("${start.fullyQualifiedName} is not present within the graph")
      val endNode = graph.getVertices(QUALIFIED_NAME, target.fullyQualifiedName).toList().firstOrNull() ?: throw IllegalArgumentException("${target.fullyQualifiedName} is not present within the graph")
      val sql = """
         SELECT expand(path) FROM (
           SELECT shortestPath(${startNode.id}, ${endNode.id}) AS path
           UNWIND path
         )
         """
      val path = graph.command(OCommandSQL(sql)).execute<Iterable<OrientVertex>>().toList()
      val links = convertToLinks(path)
      return Path(start, target, links)
   }

   private fun convertToLinks(path: List<OrientVertex>): List<Link> {
      val links = path.mapIndexed { index, vertex ->
         if (index + 1 >= path.size) {
            null
         } else {
            val fromNode = vertex
            val toNode = path[index + 1]
            val edge = fromNode.getEdges(toNode, Direction.OUT).toList().firstOrNull() ?: throw IllegalStateException("No edge found from ${fromNode[QUALIFIED_NAME]} -> ${toNode[QUALIFIED_NAME]}, but they were adjoining nodes in the shortestPath query")
            val relationship = Relationship.valueOf(edge.label)
            Link(fromNode[QUALIFIED_NAME].fqn(), relationship, toNode[QUALIFIED_NAME].fqn())
         }

      }.filterNotNull()
      return links
   }
}
