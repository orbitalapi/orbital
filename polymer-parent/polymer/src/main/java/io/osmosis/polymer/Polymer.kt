package io.osmosis.polymer

import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.OCommandSQL
import com.tinkerpop.blueprints.Direction
import com.tinkerpop.blueprints.Edge
import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.blueprints.impls.orient.OrientEdge
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx
import com.tinkerpop.blueprints.impls.orient.OrientVertex
import io.osmosis.polymer.GraphAttributes.EDGE_KEY
import io.osmosis.polymer.GraphAttributes.NODE_TYPE
import io.osmosis.polymer.GraphAttributes.QUALIFIED_NAME
import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.query.QueryContext
import io.osmosis.polymer.query.QueryEngineFactory
import io.osmosis.polymer.query.StatefulQueryEngine
import io.osmosis.polymer.schemas.*
import io.osmosis.polymer.utils.log

object GraphAttributes {
   val NODE_TYPE = "nodeType"
   val QUALIFIED_NAME = "qualifiedName"
   val EDGE_KEY = "edgeKey"
}

enum class NodeTypes {
   ATTRIBUTE,
   TYPE,
   OBJECT,
   SERVICE
}

typealias OperationReference = String

interface SchemaContainer {
   val schema: Schema
   fun getType(typeName: String): Type = schema.type(typeName)
}

interface ModelContainer : SchemaContainer {
   fun addModel(model: TypedInstance): ModelContainer
}

class Polymer(schemas: List<Schema>, private val graph: OrientGraphNoTx, private val queryEngineFactory: QueryEngineFactory) : SchemaPathResolver, ModelContainer {
   private val schemas = mutableListOf<Schema>()
   private val models = mutableSetOf<TypedInstance>()


   override var schema: Schema = CompositeSchema(schemas)
      private set

   fun query(): StatefulQueryEngine {
      return queryEngineFactory.queryEngine(schema, models, this)
   }

   fun models(): Set<TypedInstance> {
      return models.toSet()
   }

   fun export(): String {
      return DbExporter(graph).export()
   }

   fun export(path: String): String {
      return DbExporter(graph).export(path)
   }


//   fun queryContext(): QueryContext = QueryContext(schema, facts, this)

   constructor(queryEngineFactory: QueryEngineFactory = QueryEngineFactory.default(), graphConnectionString: String = "remote:localhost/test") : this(emptyList(), OrientGraphFactory(graphConnectionString).setupPool(1, 100).noTx, queryEngineFactory)

   override fun addModel(model: TypedInstance): Polymer {
      log().debug("Added model instance to context: $model")
      models.add(model)
      return this
   }

   fun addSchema(schema: Schema): Polymer {
      schemas.add(schema)
      this.schema = CompositeSchema(schemas)
      appendToGraph(schema)
      return this
   }

   private fun appendToGraph(schema: Schema) {
//      val attributes = appendAttributes(schema)
      val types = appendTypes(schema)
      val services = appendServices(schema)

//      val nodes: Map<QualifiedName, Vertex> = types + attributes
//      schema.links.forEach { link ->
//         graph.addEdge(null, nodes[link.start], nodes[link.end], link.relationship.name)
//         log().debug("Defined link $link")
      //         graph.addEdge(null, )
//      }

   }

   private fun appendServices(schema: Schema): Map<OperationReference, Pair<Service, Operation>> {
      return schema.services.flatMap { service: Service ->
         service.operations.map { operation: Operation ->
            val operationReference = "${service.qualifiedName}@@${operation.name}"
            val operationNode = addVertex(operationReference, NodeTypes.SERVICE)
            operation.parameters.forEach { parameter ->
               val typeFqn = parameter.type.name.fullyQualifiedName
               val parameterTypeNode = findVertex(typeFqn) ?: throw IllegalArgumentException("Type $typeFqn is specified as a param for $operationReference, but is not defined in the existing schema")
               operationNode.linkTo(parameterTypeNode, Relationship.REQUIRES_PARAMETER)
               parameterTypeNode.linkTo(operationNode, Relationship.IS_PARAMETER_ON)
            }
            val resultTypeFqn = operation.returnType.name.fullyQualifiedName
            val resultTypeNode = findVertex(resultTypeFqn) ?: throw IllegalArgumentException("Type $resultTypeFqn is specified as the return type for $operationReference, but is not defined in the existing schema")
            operationNode.linkTo(resultTypeNode, Relationship.PROVIDES)

            operationReference to (service to operation)
         }
      }.toMap()
   }

   private fun appendTypes(schema: Schema): Map<QualifiedName, Vertex> {
      schema.types.map { type: Type ->
         val typeFullyQualifiedName = type.name.fullyQualifiedName
         val typeNode = addVertex(type.name, NodeTypes.OBJECT)
         type.attributes.map { (attributeName, attributeType) ->
            val attributeQualifiedName = "$typeFullyQualifiedName/$attributeName"
            val (attributeVertex, _) = addVertex(attributeQualifiedName, NodeTypes.ATTRIBUTE).linkTo(typeNode, Relationship.IS_ATTRIBUTE_OF)
            typeNode.linkTo(attributeVertex, Relationship.HAS_ATTRIBUTE)
            val attributeTypeVertex = addVertex(attributeType.name, NodeTypes.TYPE)
            attributeTypeVertex.linkFrom(attributeVertex, Relationship.IS_TYPE_OF)
            attributeTypeVertex.linkTo(attributeVertex, Relationship.HAS_PARAMETER_OF_TYPE)

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

   private fun findVertex(name: String): Vertex? {
      val existing = graph.getVertices(QUALIFIED_NAME, name).toList()
      if (existing.isEmpty()) {
         return null
      } else if (existing.size == 1) {
         return existing.first()
      } else {
         throw IllegalArgumentException("$name matched multiple nodes, should be unique")
      }

   }

   // TODO : DRY this up.
   private fun Vertex.linkTo(inVertex: Vertex, relationship: Relationship): Pair<Vertex, Edge> {
      val existingEdge = getExistingEdgeIfPresent(inVertex,this,relationship)
      return if (existingEdge != null) {
         this to existingEdge
      } else {
         val edge = graph.addEdge(null, this, inVertex, relationship.name)
         edge.setProperty(EDGE_KEY, getEdgeKey(this,inVertex, relationship))
         log().debug("Added edge from ${this[QUALIFIED_NAME]} -[${relationship.name}]-> ${inVertex[QUALIFIED_NAME]}  (${this.id} -> ${inVertex.id})")
         this to edge
      }
   }

   // TODO : DRY this up.
   private fun Vertex.linkFrom(outVertex: Vertex, relationship: Relationship): Pair<Vertex, Edge> {
      val existingEdge = getExistingEdgeIfPresent(this,outVertex,relationship)
      return if (existingEdge != null) {
         this to existingEdge
      } else {
         val edge = graph.addEdge(null, outVertex, this, relationship.name)
         edge.setProperty(EDGE_KEY, getEdgeKey(outVertex,this, relationship))
         log().debug("Added edge from ${outVertex[QUALIFIED_NAME]} -[${relationship.name}]-> ${this[QUALIFIED_NAME]} (${outVertex.id} -> ${this.id})")
         return this to edge
      }

   }

   private fun getExistingEdgeIfPresent(inVertex: Vertex, outVertex: Vertex, relationship: Relationship):Edge? {
      val edgeKey = getEdgeKey(outVertex, inVertex, relationship)
      val existingEdges = graph.getEdges(EDGE_KEY, edgeKey).toList()
      return existingEdges.firstOrNull()
   }

   private fun getEdgeKey(outVertex: Vertex, inVertex: Vertex, relationship: Relationship): String {
      return "${outVertex.id}-[${relationship.name}]->${inVertex.id}"
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

   fun from(s: TypedInstance): QueryContext {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   override fun findPath(start: String, target: String): Path {
      return findPath(start.fqn(), target.fqn())
   }

   override fun findPath(start: Type, target: Type): Path = findPath(start.name, target.name)

   override fun findPath(start: QualifiedName, target: QualifiedName): Path {

      log().debug("Searching for path from $start -> $target")
      // TODO : This is very hacky
      val startNode = graph.getVertices(QUALIFIED_NAME, start.fullyQualifiedName).toList().firstOrNull() ?: throw IllegalArgumentException("${start.fullyQualifiedName} is not present within the graph")
      val endNode = graph.getVertices(QUALIFIED_NAME, target.fullyQualifiedName).toList().firstOrNull() ?: throw IllegalArgumentException("${target.fullyQualifiedName} is not present within the graph")
      val sql = """
SELECT expand(path) FROM (
  SELECT shortestPath(${startNode.id}, ${endNode.id}) AS path
  UNWIND path
)
         """.trim()
      val path = graph.command(OCommandSQL(sql)).execute<Iterable<OrientVertex>>().toList()
      val links = convertToLinks(path)
      log().debug(sql)
      val resolvedPath = Path(start, target, links)
      if (resolvedPath.exists) {
         log().debug("Path from $start -> $target found with ${resolvedPath.links.size} links")
         log().debug(resolvedPath.description)
      } else {
         log().debug("Path from $start -> $target not found")
      }
      return resolvedPath
   }

   private fun convertToLinks(path: List<OrientVertex>): List<Link> {
      val links = path.mapIndexed { index, vertex ->
         if (index + 1 >= path.size) {
            null
         } else {
            val fromNode = vertex
            val toNode = path[index + 1]
            val edge = fromNode.getEdges(toNode, Direction.BOTH).toList().firstOrNull() as OrientEdge ?: throw IllegalStateException("No edge found from ${fromNode[QUALIFIED_NAME]} -> ${toNode[QUALIFIED_NAME]}, but they were adjoining nodes in the shortestPath query")

            val inboundNode = edge.inVertex as ODocument
            val outboundNode = edge.outVertex as ODocument
            val relationship = Relationship.valueOf(edge.label)
            Link(outboundNode.field<String>(QUALIFIED_NAME).fqn(), relationship, inboundNode.field<String>(QUALIFIED_NAME).fqn())
         }

      }.filterNotNull()
      return links
   }

   //   fun getType(typeName: String): Type = schema.type(typeName)
   fun type(typeName: String): Type = getType(typeName)

   fun getService(serviceName: String): Service = schema.service(serviceName)
}
