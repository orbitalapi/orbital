package io.vyne.query

import com.diffplug.common.base.TreeDef
import com.diffplug.common.base.TreeStream
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.collect.HashMultimap
import io.vyne.models.*
import io.vyne.query.FactDiscoveryStrategy.TOP_LEVEL_ONLY
import io.vyne.query.graph.EvaluatedEdge
import io.vyne.schemas.*
import io.vyne.utils.log
import lang.taxi.policies.Instruction
import java.util.*
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

data class QuerySpecTypeNode(val type: Type, val children: Set<QuerySpecTypeNode> = emptySet(), val mode: QueryMode = QueryMode.DISCOVER)

data class QueryResult(
   @field:JsonIgnore // we send a lightweight version below
   val results: Map<QuerySpecTypeNode, TypedInstance?>,
   @field:JsonIgnore // we send a lightweight version below
   val unmatchedNodes: Set<QuerySpecTypeNode> = emptySet(),
   val path: Path? = null,
   @field:JsonIgnore // this sends too much information - need to build a lightweight version
   override val profilerOperation: ProfilerOperation? = null,
   override val queryResponseId: String = UUID.randomUUID().toString(),
   override val resultMode: ResultMode
) : QueryResponse {

   val duration = profilerOperation?.duration

   override val isFullyResolved = unmatchedNodes.isEmpty()
   operator fun get(typeName: String): TypedInstance? {
      val requestedParameterizedName = typeName.fqn().parameterizedName
      // TODO : THis should consider inheritence, rather than strict equals
      return this.results.filterKeys { it.type.name.parameterizedName == requestedParameterizedName }
         .values
         .first()
   }

   operator fun get(type: Type): TypedInstance? {
      return this.results.filterKeys { it.type == type }
         .values
         .first()
   }

   @JsonProperty("unmatchedNodes")
   val unmatchedNodeNames: List<QualifiedName> = this.unmatchedNodes.map { it.type.name }

   // The result map is structured so the key is the thing that was asked for, and the value
   // is a TypeNamedInstance of the result.
   // By including the type in both places, it allows for polymorphic return types.
   // Also, the reason we're using Any for the value is that the result could be a
   // TypedInstnace, a map of TypedInstnaces, or a collection of TypedInstances.
   @JsonProperty("results")
   val resultMap: Map<String, Any?> =
      when(resultMode)
      {
         ResultMode.VERBOSE -> this.results.map { (key, value) ->
            val mapped = value?.toTypeNamedInstance()
            key.type.name.parameterizedName to mapped
         }.toMap()

         ResultMode.SIMPLE -> this.results
            .map { (key, value) -> key.type.name.parameterizedName to value?.toRawObject() }
            .toMap()
      }

}

// Note : Also models failures, so is fairly generic
interface QueryResponse {
   val queryResponseId: String
   val isFullyResolved: Boolean
   val profilerOperation: ProfilerOperation?
   val remoteCalls: List<RemoteCall>
      get() = collateRemoteCalls(this.profilerOperation)

   val timings: Map<OperationType, Long>
      get() {
         return profilerOperation?.timings ?: emptyMap()
      }

   val vyneCost: Long
      get() = profilerOperation?.vyneCost ?: 0L

   val resultMode: ResultMode
}

fun collateRemoteCalls(profilerOperation: ProfilerOperation?): List<RemoteCall> {
   if (profilerOperation == null) return emptyList()
   return profilerOperation.remoteCalls + profilerOperation.children.flatMap { collateRemoteCalls(it) }
}

data class TypeNameAndValue(
   val typeName: String,
   val value: Any?
)

object TypedInstanceTree {
   /**
    * Function which defines how to convert a TypedInstance into a tree, for traversal
    */
   val treeDef: TreeDef<TypedInstance> = TreeDef.of { instance: TypedInstance ->

      // This is a naieve first pass, and I doubt this wil work.
      // For example, how will we ever use the values within?
      if (instance.type.isClosed) {
         return@of emptyList<TypedInstance>()
      }
      when (instance) {
         is TypedObject -> instance.values.toList()
         is TypedValue -> emptyList()
         is TypedCollection -> instance.value
         else -> throw IllegalStateException("TypedInstance of type ${instance.javaClass.simpleName} is not handled")
      }
      // TODO : How do we handle nulls here?  For now, they're remove, but this is misleading, since we have a typedinstnace, but it's value is null.
   }.filter { it -> it !is TypedNull }
}

// Design choice:
// Query Context's don't have a concept of FactSets, everything is just flattened to facts.
// However, the QueryEngineFactory DOES retain the concept.
// This is because by the time you go to run a query, you should be focussed on
// "What do I know", and not "Where did I learn this?"
// At one point, the FactSetMap leaked down to QueryContext and beyond, and this caused
// many different classes to have to become aware of multiple factsets, which felt like a leak.
// Revisit if the above becomes less true.
data class QueryContext(
   val schema: Schema,
   val facts: MutableSet<TypedInstance>,
   val queryEngine: QueryEngine,
   val profiler: QueryProfiler,
   val resultMode: ResultMode) : ProfilerOperation by profiler {
   private val evaluatedEdges = mutableListOf<EvaluatedEdge>()
   private val policyInstructionCounts = mutableMapOf<Pair<QualifiedName, Instruction>, Int>()

   fun find(typeName: String): QueryResult = find(TypeNameQueryExpression(typeName))

   fun find(queryString: QueryExpression): QueryResult = queryEngine.find(queryString, this)
   fun find(target: QuerySpecTypeNode): QueryResult = queryEngine.find(target, this)
   fun find(target: Set<QuerySpecTypeNode>): QueryResult = queryEngine.find(target, this)

   fun build(typeName: String): QueryResult = queryEngine.build(TypeNameQueryExpression(typeName), this)

   fun gather(typeName: String): QueryResult = gather(TypeNameQueryExpression(typeName))
   fun gather(queryString: QueryExpression): QueryResult = queryEngine.gather(queryString, this)


   companion object {
      fun from(schema: Schema, facts: Set<TypedInstance>, queryEngine: QueryEngine, profiler: QueryProfiler, resultMode: ResultMode) =
         QueryContext(schema, facts.toMutableSet(), queryEngine, profiler, resultMode)
   }

   fun addFact(fact: TypedInstance) {
      log().debug("Added fact to queryContext: ${fact.type.fullyQualifiedName}")
      this.facts.add(fact)
   }

   fun addFacts(facts: Collection<TypedInstance>) {
      facts.forEach { this.addFact(it) }
   }


   fun addEvaluatedEdge(evaluatedEdge: EvaluatedEdge) = this.evaluatedEdges.add(evaluatedEdge)

   // Wraps all the known facts under a root node, turning it into a tree
   private fun dataTreeRoot(): TypedCollection = TypedCollection(Type("osmosis.internal.RootNode".fqn(), sources = listOf(SourceCode.undefined("NoLang")), typeDoc = null), facts.toList())

   /**
    * A breadth-first stream of data facts currently held in the collection.
    * Use breadth-first, as we want to favour nodes closer to the root.
    * Deeply nested children are less likely to be relevant matches.
    */
   fun modelTree(): Stream<TypedInstance> {
      // TODO : How do we handle nulls here?  For now, they're remove, but this is misleading, since we have a typedinstnace, but it's value is null.
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

   fun addAppliedInstruction(policy: Policy, instruction: Instruction) {
      policyInstructionCounts.compute(policy.name to instruction) { _, atomicInteger -> if (atomicInteger != null) atomicInteger + 1 else 1 }
   }
}


enum class FactDiscoveryStrategy {
   TOP_LEVEL_ONLY {
      override fun getFact(context: QueryContext, type: Type, matcher: TypeMatchingStrategy): TypedInstance? = context.facts.firstOrNull { matcher.matches(type, it.type) }
   },

   /**
    * Will return a match from any depth, providing there is
    * exactly one match in the context
    */
   ANY_DEPTH_EXPECT_ONE {
      override fun getFact(context: QueryContext, type: Type, matcher: TypeMatchingStrategy): TypedInstance? {
         val matches = context.modelTree()
            .filter { matcher.matches(type, it.type) }
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
      override fun getFact(context: QueryContext, type: Type, matcher: TypeMatchingStrategy): TypedInstance? {
         val matches = context.modelTree()
            .filter { matcher.matches(type, it.type) }
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
   },

   /**
    * Will return matches from any depth, providing there is exactly
    * one DISITNCT match within the context
    */
   ANY_DEPTH_ALLOW_MANY {
      override fun getFact(context: QueryContext, type: Type, matcher: TypeMatchingStrategy): TypedCollection? {
         val matches = context.modelTree()
            .filter { matcher.matches(type, it.type) }
            .distinct()
            .toList()
         return when {
            matches.isEmpty() -> null
            else -> TypedCollection.from(matches)
         }
      }
   };


   abstract fun getFact(context: QueryContext, type: Type, strictness: TypeMatchingStrategy = TypeMatchingStrategy.ALLOW_INHERITED_TYPES): TypedInstance?

}

enum class TypeMatchingStrategy {
   ALLOW_INHERITED_TYPES {
      override fun matches(requestedType: Type, candidate: Type): Boolean {
         return requestedType.fullyQualifiedName == candidate.fullyQualifiedName ||
            candidate.inheritanceGraph.map { it.fullyQualifiedName }.contains(requestedType.fullyQualifiedName)
      }
   },
   EXACT_MATCH {
      override fun matches(requestedType: Type, candidate: Type): Boolean {
         return requestedType.fullyQualifiedName == candidate.fullyQualifiedName
      }
   };


   abstract fun matches(requestedType: Type, candidate: Type): Boolean

}

fun <K, V> HashMultimap<K, V>.copy(): HashMultimap<K, V> {
   return HashMultimap.create(this)
}
