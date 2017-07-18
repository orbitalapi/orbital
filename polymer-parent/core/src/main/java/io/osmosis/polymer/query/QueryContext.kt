package io.osmosis.polymer.query

import com.diffplug.common.base.TreeDef
import com.diffplug.common.base.TreeStream
import io.osmosis.polymer.Polymer
import io.osmosis.polymer.SchemaPathResolver
import io.osmosis.polymer.models.TypedCollection
import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.models.TypedObject
import io.osmosis.polymer.models.TypedValue
import io.osmosis.polymer.schemas.Schema
import io.osmosis.polymer.schemas.Type
import io.osmosis.polymer.utils.log
import java.util.stream.Stream

/**
 * Defines a node within a QuerySpec that
 * describes the expected return type.
 * eg:
 * Given
 * {
 *    Client {  // <---QuerySpecTypeNode
 *       ClientId, ClientFirstName, ClientLastName // <--- 3 Children, all QuerySpecTypeNode's too!
 *    }
 * }
 *
 */
data class QuerySpecTypeNode(val type: Type, val children: Set<QuerySpecTypeNode> = emptySet())

data class QueryResult(val results: Map<QuerySpecTypeNode, TypedInstance?>, val unmatchedNodes: Set<QuerySpecTypeNode> = emptySet()) {
   val isFullyResolved = unmatchedNodes.isEmpty()
   operator fun get(typeName: String): TypedInstance? {
      return this.results.filterKeys { it.type.name.fullyQualifiedName == typeName }
         .values
         .first()
   }
}

data class QueryContext(val schema: Schema, val models: Set<TypedInstance>, private val polymer: Polymer) : SchemaPathResolver by polymer {
   /**
    * Function which defines how to convert a TypedInstance into a tree, for traversal
    */
   private val typedInstanceTreeDef: TreeDef<TypedInstance> = TreeDef.of { instance: TypedInstance ->
      when (instance) {
         is TypedObject -> instance.values.toList()
         is TypedValue -> emptyList()
         is TypedCollection -> instance.value
         else -> throw IllegalStateException("TypedInstance of type ${instance.javaClass.simpleName} is not handled")
      }
   }

   // Wraps all the known models under a root node, turning it into a tree
   private val dataTreeRoot: TypedCollection = TypedCollection(Type("osmosis.internal.RootNode"), models.toList())

   /**
    * A breadth-first stream of data models currently held in the collection
    */
   fun modelTree(): Stream<TypedInstance> {
      return TreeStream.breadthFirst(typedInstanceTreeDef, dataTreeRoot)
   }
}

class QueryEngine(private val context: QueryContext, private val strategies: List<QueryStrategy>) {
   fun find(target: QuerySpecTypeNode): QueryResult {
      return find(setOf(target))
   }

   fun find(target: Set<QuerySpecTypeNode>): QueryResult {
      val matchedNodes = mutableMapOf<QuerySpecTypeNode, TypedInstance?>()
      // This is cheating, probably.
      // We only resolve top-level nodes, rather than traverse deeply.
      fun unresolvedNodes(): List<QuerySpecTypeNode> {
         return target.filterNot { matchedNodes.containsKey(it) }
      }

      val iterator = strategies.iterator()
      while (iterator.hasNext() && unresolvedNodes().isNotEmpty()) {
         val strategyResult: QueryStrategyResult = iterator.next().invoke(target, context)
         matchedNodes.putAll(strategyResult.matchedNodes)
         if (strategyResult.additionalData.isNotEmpty()) {
            TODO("Should add to the context, and start querying again, as new resolutions may now be possible")
         }
      }
      if (unresolvedNodes().isNotEmpty()) {
         log().error("The following nodes weren't matched: ${unresolvedNodes().joinToString { ", " }}")
      }
      return QueryResult(matchedNodes, unresolvedNodes().toSet())
   }

   fun find(queryString: String): QueryResult {
      return find(QueryParser(context.schema).parse(queryString))
   }
}

