package io.osmosis.polymer.query

import com.diffplug.common.base.TreeDef
import com.diffplug.common.base.TreeStream
import io.osmosis.polymer.models.TypedCollection
import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.models.TypedObject
import io.osmosis.polymer.models.TypedValue
import io.osmosis.polymer.schemas.Schema
import io.osmosis.polymer.schemas.Type
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
   operator fun get(type:Type):TypedInstance? {
      return this.results.filterKeys { it.type == type }
         .values
         .first()
   }
}

object TypedInstanceTree {
   /**
    * Function which defines how to convert a TypedInstance into a tree, for traversal
    */
   val treeDef: TreeDef<TypedInstance> = TreeDef.of { instance: TypedInstance ->
      when (instance) {
         is TypedObject -> instance.values.toList()
         is TypedValue -> emptyList()
         is TypedCollection -> instance.value
         else -> throw IllegalStateException("TypedInstance of type ${instance.javaClass.simpleName} is not handled")
      }
   }
}

data class QueryContext(val schema: Schema, val facts: MutableSet<TypedInstance>, val queryEngine: QueryEngine) {
   private val factsByType
      get() = facts.associateBy { it.type }

   companion object {
      fun from(schema: Schema, facts: Set<TypedInstance>, queryEngine: QueryEngine) = QueryContext(schema, facts.toMutableSet(), queryEngine)
   }

   fun addFact(fact: TypedInstance) {
      this.facts.add(fact)
   }

   fun addFacts(facts: Collection<TypedInstance>) {
      this.facts.addAll(facts)
   }

   // Wraps all the known facts under a root node, turning it into a tree
   private val dataTreeRoot: TypedCollection = TypedCollection(Type("osmosis.internal.RootNode"), facts.toList())

   /**
    * A breadth-first stream of data facts currently held in the collection.
    * Use breadth-first, as we want to favour nodes closer to the root.
    * Deeply nested children are less likely to be relevant matches.
    */
   fun modelTree(): Stream<TypedInstance> {
      return TreeStream.breadthFirst(TypedInstanceTree.treeDef, dataTreeRoot)
   }

   fun hasFactOfType(type: Type): Boolean {
      return factsByType.containsKey(type)
   }

   fun getFact(type: Type): TypedInstance {
      return factsByType[type]!!
   }

}

