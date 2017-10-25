package io.osmosis.polymer.query

import com.diffplug.common.base.TreeDef
import com.diffplug.common.base.TreeStream
import io.osmosis.polymer.models.TypedCollection
import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.models.TypedObject
import io.osmosis.polymer.models.TypedValue
import io.osmosis.polymer.query.FactDiscoveryStrategy.TOP_LEVEL_ONLY
import io.osmosis.polymer.query.graph.EvaluatedEdge
import io.osmosis.polymer.schemas.Path
import io.osmosis.polymer.schemas.Schema
import io.osmosis.polymer.schemas.Type
import io.osmosis.polymer.utils.log
import java.util.stream.Stream
import kotlin.streams.toList

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
// TODO : Why isn't the type enough, given that has children?  Why do I need to explicitly list the children I want?
data class QuerySpecTypeNode(val type: Type, val children: Set<QuerySpecTypeNode> = emptySet())

data class QueryResult(val results: Map<QuerySpecTypeNode, TypedInstance?>, val unmatchedNodes: Set<QuerySpecTypeNode> = emptySet(), val path: Path?) {
   val isFullyResolved = unmatchedNodes.isEmpty()
   operator fun get(typeName: String): TypedInstance? {
      return this.results.filterKeys { it.type.name.fullyQualifiedName == typeName }
         .values
         .first()
   }

   operator fun get(type: Type): TypedInstance? {
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

data class QueryContext(override val schema: Schema, val facts: MutableSet<TypedInstance>, val queryEngine: QueryEngine, val profiler:QueryProfiler) : QueryEngine by queryEngine {
   private val operationRecorder = profiler.startOperation("Query")
   private val evaluatedEdges = mutableListOf<EvaluatedEdge>()
   private val factsByType
      get() = facts.associateBy { it.type }

   companion object {
      fun from(schema: Schema, facts: Set<TypedInstance>, queryEngine: QueryEngine, profiler: QueryProfiler) = QueryContext(schema, facts.toMutableSet(), queryEngine, profiler)
   }

   fun addFact(fact: TypedInstance) {
      log().debug("Added fact to queryContext: $fact")
      this.facts.add(fact)
   }

   fun addFacts(facts: Collection<TypedInstance>) {
      facts.forEach { this.addFact(it) }
   }

   fun addEvaluatedEdge(evaluatedEdge: EvaluatedEdge) = this.evaluatedEdges.add(evaluatedEdge)

   // Wraps all the known facts under a root node, turning it into a tree
   private fun dataTreeRoot(): TypedCollection = TypedCollection(Type("osmosis.internal.RootNode"), facts.toList())

   /**
    * A breadth-first stream of data facts currently held in the collection.
    * Use breadth-first, as we want to favour nodes closer to the root.
    * Deeply nested children are less likely to be relevant matches.
    */
   fun modelTree(): Stream<TypedInstance> {
      return TreeStream.breadthFirst(TypedInstanceTree.treeDef, dataTreeRoot())
   }

   fun hasFactOfType(name: String, strategy: FactDiscoveryStrategy = TOP_LEVEL_ONLY): Boolean {
      return hasFactOfType(schema.type(name), strategy)
   }

   fun hasFactOfType(type: Type, strategy: FactDiscoveryStrategy = TOP_LEVEL_ONLY): Boolean {
      // This could be optimized, as we're searching twice for everything, and not caching anything
      return strategy.getFact(this, type) != null
   }

   fun getFact(type: Type, strategy: FactDiscoveryStrategy = TOP_LEVEL_ONLY): TypedInstance {
      // This could be optimized, as we're searching twice for everything, and not caching anything
      return strategy.getFact(this, type)!!
   }

   fun evaluatedPath(): List<EvaluatedEdge> {
      return evaluatedEdges.toList()
   }

   fun collectVisitedInstanceNodes(): Set<TypedInstance> {
      return emptySet()
//      TODO()
//      return this.evaluatedEdges.flatMap {
//         it.elements.filter { it.elementType == ElementType.INSTANCE }
//            .map { it.value as TypedInstance }
//      }.toSet()
   }
}



enum class FactDiscoveryStrategy {
   TOP_LEVEL_ONLY {
      override fun getFact(context: QueryContext, type: Type): TypedInstance? = context.facts.firstOrNull { it.type == type }
   },

   /**
    * Will return a match from any depth, providing there is
    * exactly one match in the context
    */
   ANY_DEPTH_EXPECT_ONE {
      override fun getFact(context: QueryContext, type: Type): TypedInstance? {
         val matches = context.modelTree()
            .filter { it.type == type }
            .toList()
         return when {
            matches.isEmpty() -> null
            matches.size == 1 -> matches.first()
            else -> {
               log().debug("ANY_DEPTH_EXPECT_ONE strategy found ${matches.size} of type ${type.name}, so returning null")
               null
            }

         }
      }
   },

   /**
    * Will return matches from any depth, providing there is exactly
    * one DISITNCT match within the context
    */
   ANY_DEPTH_EXPECT_ONE_DISTINCT {
      override fun getFact(context: QueryContext, type: Type): TypedInstance? {
         val matches = context.modelTree()
            .filter { it.type == type }
            .distinct()
            .toList()
         return when {
            matches.isEmpty() -> null
            matches.size == 1 -> matches.first()
            else -> {
               log().debug("ANY_DEPTH_EXPECT_ONE strategy found ${matches.size} of type ${type.name}, so returning null")
               null
            }
         }
      }
   };


   abstract fun getFact(context: QueryContext, type: Type): TypedInstance?

}
