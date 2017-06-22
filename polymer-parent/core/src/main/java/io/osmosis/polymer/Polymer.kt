package io.osmosis.polymer

import com.tinkerpop.blueprints.impls.orient.OrientGraph
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory
import io.osmosis.polymer.GraphAttributes.NODE_TYPE
import io.osmosis.polymer.GraphAttributes.QUALIFIED_NAME
import io.osmosis.polymer.models.json.JsonModel
import io.osmosis.polymer.schemas.Path
import io.osmosis.polymer.schemas.QualifiedName
import io.osmosis.polymer.schemas.Schema
import io.osmosis.polymer.schemas.fqn
import io.osmosis.polymer.utils.log

object GraphAttributes {
   val NODE_TYPE = "nodeType"
   val QUALIFIED_NAME = "qualifiedName"
}

enum class NodeTypes {
   ATTRIBUTE,
   TYPE
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
      appendAttributes(schema)
      appendTypes(schema)

      schema.links.forEach { link ->
      }

   }

   private fun appendTypes(schema: Schema) {
      schema.types.forEach { type ->
         graph.addVertex(type.name,
            mapOf(
               QUALIFIED_NAME to type.name,
               NODE_TYPE to NodeTypes.TYPE)
         )
         log().debug("Added attribute ${type.name} to graph")
      }
   }

   private fun appendAttributes(schema: Schema) {
      schema.attributes.forEach { name ->
         graph.addVertex(name,
            mapOf(
               QUALIFIED_NAME to name,
               NODE_TYPE to NodeTypes.ATTRIBUTE)
         )
         log().debug("Added attribute $name to graph")
      }
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
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }
}
