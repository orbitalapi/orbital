package io.vyne.query

import com.diffplug.common.base.TreeDef
import com.diffplug.common.base.TreeStream
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.KeyDeserializer
import com.google.common.collect.HashMultimap
import io.vyne.models.*
import io.vyne.query.FactDiscoveryStrategy.TOP_LEVEL_ONLY
import io.vyne.query.ProjectionAnonymousTypeProvider.projectedTo
import io.vyne.query.QueryResponse.ResponseStatus
import io.vyne.query.QueryResponse.ResponseStatus.COMPLETED
import io.vyne.query.QueryResponse.ResponseStatus.INCOMPLETE
import io.vyne.query.graph.Element
import io.vyne.query.graph.EvaluatableEdge
import io.vyne.query.graph.EvaluatedEdge
import io.vyne.query.graph.ServiceAnnotations
import io.vyne.schemas.*
import io.vyne.utils.log
import io.vyne.vyneql.ProjectedType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import lang.taxi.policies.Instruction
import lang.taxi.types.EnumType
import lang.taxi.types.PrimitiveType
import java.util.*
import java.util.stream.Stream
import kotlin.streams.toList
import java.io.PrintWriter

import java.io.StringWriter




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
@JsonInclude(JsonInclude.Include.NON_NULL)
data class QuerySpecTypeNode(
   val type: Type,
   val children: Set<QuerySpecTypeNode> = emptySet(),
   val mode: QueryMode = QueryMode.DISCOVER,
   // Note: Not really convinced these need to be OutputCOnstraints (vs Constraints).
   // Revisit later
   val dataConstraints: List<OutputConstraint> = emptyList()
) {
   val description = type.longDisplayName
}

class QueryResultResultsAttributeKeyDeserialiser : KeyDeserializer() {
   override fun deserializeKey(p0: String?, p1: DeserializationContext?): Any? {
      return null
   }

}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class QueryResult(
   @field:JsonIgnore
   val type:QuerySpecTypeNode,
   @field:JsonIgnore // we send a lightweight version below
   val results: Flow<TypedInstance>?,
   @field:JsonIgnore // we send a lightweight version below
   val unmatchedNodes: Set<QuerySpecTypeNode> = emptySet(),
   val path: Path? = null,
   @field:JsonIgnore // this sends too much information - need to build a lightweight version
   override val profilerOperation: ProfilerOperation? = null,
   override val queryResponseId: String = UUID.randomUUID().toString(),
   val truncated: Boolean = false,
   val anonymousTypes: Set<Type> = setOf()
) : QueryResponse {

   val duration = profilerOperation?.duration

   override val isFullyResolved = unmatchedNodes.isEmpty()
   override val responseStatus: ResponseStatus = if (isFullyResolved) COMPLETED else INCOMPLETE

   // TODO Does this make sense given the anymore?
   operator fun get(typeName: String): Flow<TypedInstance>? {
      val requestedParameterizedName = typeName.fqn().parameterizedName
      // TODO : THis should consider inheritence, rather than strict equals
      return this.results
   }

   operator fun get(type: Type): Flow<TypedInstance>? {
      return this.results?.filter { it.type == type }
   }

   @JsonProperty("unmatchedNodes")
   val unmatchedNodeNames: List<QualifiedName> = this.unmatchedNodes.map { it.type.name }


   // The result map is structured so the key is the thing that was asked for, and the value
   // is a TypeNamedInstance of the result.
   // By including the type in both places, it allows for polymorphic return types.
   // Also, the reason we're using Any for the value is that the result could be a
   // TypedInstnace, a map of TypedInstnaces, or a collection of TypedInstances.



   @get:JsonIgnore
   val simpleResults: Flow<TypedInstance>? by lazy {
      val converter = TypedInstanceConverter(RawObjectMapper)
      results
    }

   override fun historyRecord(): HistoryQueryResponse {
      return HistoryQueryResponse(
         null, // TODO Should a historyRecord contain results ?
         unmatchedNodeNames,
         this.isFullyResolved,
         queryResponseId,
         profilerOperation?.toDto(),
         responseStatus,
         remoteCalls,
         timings
      )
   }
}

// Note : Also models failures, so is fairly generic
interface QueryResponse {
   enum class ResponseStatus {
      COMPLETED,

      // Ie., the query didn't error, but not everything was resolved
      INCOMPLETE,
      ERROR,
   }

   val responseStatus: ResponseStatus
   val queryResponseId: String

   @get:JsonProperty("fullyResolved")
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

   fun historyRecord(): HistoryQueryResponse
}

fun collateRemoteCalls(profilerOperation: ProfilerOperation?): List<RemoteCall> {
   if (profilerOperation == null) return emptyList()
   return profilerOperation.remoteCalls + profilerOperation.children.flatMap { collateRemoteCalls(it) }
}

object TypedInstanceTree {
   /**
    * Function which defines how to convert a TypedInstance into a tree, for traversal
    */

      fun visit(instance: TypedInstance):List<TypedInstance> {

         if (instance.type.isClosed) {
            return emptyList()
         }

         return when (instance) {
            is TypedObject -> instance.values.toList()
            is TypedEnumValue -> instance.synonyms
            is TypedValue -> {
               if (instance.type.isEnum) {
                  instance.type.enumTypedInstance(instance.value).synonyms
               } else {
                  emptyList()
               }

            }
            is TypedCollection -> instance.value
            else -> throw IllegalStateException("TypedInstance of type ${instance.javaClass.simpleName} is not handled")

            // TODO : How do we handle nulls here?  For now, they're remove, but this is misleading, since we have a typedinstnace, but it's value is null.
         }.filter { it -> it !is TypedNull }
      }
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
   val debugProfiling: Boolean = false,
   val parent: QueryContext? = null
) : ProfilerOperation by profiler {

   private val evaluatedEdges = mutableListOf<EvaluatedEdge>()
   private val policyInstructionCounts = mutableMapOf<Pair<QualifiedName, Instruction>, Int>()
   var isProjecting = false
   private var projectResultsTo: Type? = null
   private var inMemoryStream: List<TypedInstance>? = null

   override fun toString() = "# of facts=${facts.size} #schema types=${schema.types.size}"
   suspend fun find(typeName: String): QueryResult = find(TypeNameQueryExpression(typeName))

   suspend fun find(queryString: QueryExpression): QueryResult = queryEngine.find(queryString, this)
   suspend fun find(target: QuerySpecTypeNode): QueryResult = queryEngine.find(target, this)
   suspend fun find(target: Set<QuerySpecTypeNode>): QueryResult = queryEngine.find(target, this)
   suspend fun find(target: QuerySpecTypeNode, excludedOperations: Set<SearchGraphExclusion<Operation>>): QueryResult =
      queryEngine.find(target, this, excludedOperations)

   suspend fun build(typeName: QualifiedName): QueryResult = build(typeName.fullyQualifiedName)
   suspend fun build(typeName: String): QueryResult = queryEngine.build(TypeNameQueryExpression(typeName), this)
   suspend fun build(expression: QueryExpression): QueryResult =
      //timed("QueryContext.build") {
         queryEngine.build(expression, this)
      //}

   suspend fun findAll(typeName: String): QueryResult = findAll(TypeNameQueryExpression(typeName))
   suspend fun findAll(queryString: QueryExpression): QueryResult = queryEngine.findAll(queryString, this)

   fun parseQuery(typeName: String) = queryEngine.parse(TypeNameQueryExpression(typeName))
   fun parseQuery(expression: QueryExpression) = queryEngine.parse(expression)

   companion object {
      fun from(
         schema: Schema,
         facts: Set<TypedInstance>,
         queryEngine: QueryEngine,
         profiler: QueryProfiler
      ): QueryContext {
         return QueryContext(schema, facts.toMutableSet(), queryEngine, profiler)
      }

   }

   /**
    * Returns a QueryContext, with only the provided fact.
    * All other parameters (queryEngine, schema, etc) are retained
    */
   fun only(fact: TypedInstance): QueryContext {
      ////// MERGE val mutableFacts = mutableSetOf<TypedInstance>()
      ////mutableFacts.add(fact)
      ////mutableFacts.addAll(resolveSynonyms(fact, schema).toMutableSet())
      ////val copiedContext = this.copy(facts = mutableFacts, parent = this)
      ////copiedContext.excludedServices.addAll(this.excludedServices)
      ////copiedContext.excludedOperations.addAll(this.schema.excludedOperationsForEnrichment())
      /////return copiedContext
      return this.copy(facts = mutableSetOf(fact), parent = this)
   }

   fun addFact(fact: TypedInstance): QueryContext {
      this.facts.add(fact)
      return this
   }

   fun addFacts(facts: Collection<TypedInstance>): QueryContext {
      facts.forEach { this.addFact(it) }
      return this
   }

   fun projectResultsTo(projectedType: ProjectedType): QueryContext {
      return projectResultsTo(projectedTo(projectedType, schema))
   }

   fun projectResultsTo(targetType: String): QueryContext {
      return projectResultsTo(ProjectedType.fromConcreteTypeOnly(lang.taxi.types.QualifiedName.from(targetType)))
   }

   private fun projectResultsTo(targetType: Type): QueryContext {
      projectResultsTo = targetType
      return this
   }

   fun addEvaluatedEdge(evaluatedEdge: EvaluatedEdge) = this.evaluatedEdges.add(evaluatedEdge)

   private val anyArrayType by lazy { schema.type(PrimitiveType.ANY) }

   // Wraps all the known facts under a root node, turning it into a tree
   private fun dataTreeRoot(): TypedInstance {
      return TypedCollection.arrayOf(anyArrayType, facts.toList())
   }

   /**
    * A breadth-first stream of data facts currently held in the collection.
    * Use breadth-first, as we want to favour nodes closer to the root.
    * Deeply nested children are less likely to be relevant matches.
    */
   fun modelTree(): Stream<TypedInstance> {
      class TreeNavigator {
         private val visitedNodes = mutableSetOf<TypedInstance>()

         fun visit(instance:TypedInstance):List<TypedInstance> {
            return if (visitedNodes.contains(instance)) {
               return emptyList()
            } else {
               visitedNodes.add(instance)
               TypedInstanceTree.visit(instance)
            }
         }
      }
      // TODO : How do we handle nulls here?  For now, they're remove, but this is misleading, since we have a typedinstnace, but it's value is null.
      val navigator = TreeNavigator()
      val treeDef:TreeDef<TypedInstance> = TreeDef.of { instance -> navigator.visit(instance) }
      return TreeStream.breadthFirst(treeDef, dataTreeRoot())
   }

   fun hasFactOfType(
      type: Type,
      strategy: FactDiscoveryStrategy = TOP_LEVEL_ONLY,
      spec: TypedInstanceValidPredicate = AlwaysGoodSpec
   ): Boolean {
      // This could be optimized, as we're searching twice for everything, and not caching anything
      return getFactOrNull(type, strategy, spec) != null
   }

   fun getFact(
      type: Type,
      strategy: FactDiscoveryStrategy = TOP_LEVEL_ONLY,
      spec: TypedInstanceValidPredicate = AlwaysGoodSpec
   ): TypedInstance {
      // This could be optimized, as we're searching twice for everything, and not caching anything
      return getFactOrNull(type, strategy, spec)!!
   }

   fun getFactOrNull(
      type: Type,
      strategy: FactDiscoveryStrategy = TOP_LEVEL_ONLY,
      spec: TypedInstanceValidPredicate = AlwaysGoodSpec
   ): TypedInstance? {
      return strategy.getFact(this, type, spec = spec)
      //return factCache.get(FactCacheKey(type.fullyQualifiedName, strategy)).orElse(null)
   }

   fun evaluatedPath(): List<EvaluatedEdge> {
      return evaluatedEdges.toList()
   }

   fun projectResultsTo(): Type? {
      return projectResultsTo
   }

   fun collectVisitedInstanceNodes(): Set<TypedInstance> {
      return emptySet()
   }

   fun addAppliedInstruction(policy: Policy, instruction: Instruction) {
      policyInstructionCounts.compute(policy.name to instruction) { _, atomicInteger -> if (atomicInteger != null) atomicInteger + 1 else 1 }
   }


   data class FactCacheKey(val fqn: String, val discoveryStrategy: FactDiscoveryStrategy)
   data class ServiceInvocationCacheKey(
      private val vertex1: Element,
      private val vertex2: Element,
      private val invocationParameter: Set<TypedInstance?>
   )

   private val operationCache: MutableMap<ServiceInvocationCacheKey, TypedInstance> = mutableMapOf()
   val excludedServices: MutableSet<SearchGraphExclusion<QualifiedName>> = mutableSetOf()
   val excludedOperations: MutableSet<Operation> = mutableSetOf()


   private fun getTopLevelContext(): QueryContext {
      return parent?.getTopLevelContext() ?: this
   }

   fun addOperationResult(
      operation: EvaluatableEdge,
      result: TypedInstance,
      callArgs: Set<TypedInstance?>
   ): TypedInstance {
      val key = ServiceInvocationCacheKey(operation.vertex1, operation.vertex2, callArgs)
      val (service, _) = OperationNames.serviceAndOperation(operation.vertex1.valueAsQualifiedName())
      val invokedService = schema.services.firstOrNull { it.name.fullyQualifiedName == service }
      onServiceInvoked((invokedService))
      getTopLevelContext().operationCache[key] = result
      log().info("Caching {} [{} -> {}]", operation, operation.previousValue?.value, result.type.qualifiedName)
      return result
   }

   fun onServiceInvoked(invokedService: Service?) {
      if (invokedService?.hasMetadata(ServiceAnnotations.Datasource.annotation) == true) {
         // This is a work-around to a search limitation.
         // Currently, Vyne will attempt to discover from any service that returns the output expected.
         // We should limit, such that if an entity decalres an Id, then we should only invoke that service if the
         // @Id is known to us.
         // We expect to remove this once search-only-on-id is completed.
         excludedServices.add(
            SearchGraphExclusion(
               "Exclude already invoked @DataSource annotated services from discovery searches",
               invokedService.name
            )
         )
      }
   }

   fun getOperationResult(operation: EvaluatableEdge, callArgs: Set<TypedInstance?>): TypedInstance? {
      val key = ServiceInvocationCacheKey(operation.vertex1, operation.vertex2, callArgs)
      return getTopLevelContext().operationCache[key]
   }

   fun hasOperationResult(operation: EvaluatableEdge, callArgs: Set<TypedInstance?>): Boolean {
      val key = ServiceInvocationCacheKey(operation.vertex1, operation.vertex2, callArgs)
      return getTopLevelContext().operationCache[key] != null
   }
}


enum class FactDiscoveryStrategy {
   TOP_LEVEL_ONLY {
      override fun getFact(
         context: QueryContext,
         type: Type,
         matcher: TypeMatchingStrategy,
         spec: TypedInstanceValidPredicate
      ): TypedInstance? {
         return context.facts.firstOrNull { matcher.matches(type, it.type) && spec.isValid(it) }
      }
   },

   /**
    * Will return a match from any depth, providing there is
    * exactly one match in the context
    */
   ANY_DEPTH_EXPECT_ONE {
      override fun getFact(
         context: QueryContext,
         type: Type,
         matcher: TypeMatchingStrategy,
         spec: TypedInstanceValidPredicate
      ): TypedInstance? {
         val matches = context.modelTree()
            .filter { matcher.matches(type, it.type) }
            .filter { spec.isValid(it) }
            .toList()
         return when {
            matches.isEmpty() -> null
            matches.size == 1 -> matches.first()
            else -> {
               log().debug(
                  "ANY_DEPTH_EXPECT_ONE strategy found {} of type {}, so returning null",
                  matches.size,
                  type.name
               )
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
      override fun getFact(
         context: QueryContext,
         type: Type,
         matcher: TypeMatchingStrategy,
         spec: TypedInstanceValidPredicate
      ): TypedInstance? {
         val matches = context.modelTree()
            .filter { matcher.matches(type, it.type) }
            .filter { spec.isValid(it) }
            .distinct()
            .toList()
         return when {
            matches.isEmpty() -> null
            matches.size == 1 -> matches.first()
            else -> {
               // last ditch attempt
               val exactMatch = matches.filter { it.type == type }
               if (exactMatch.size == 1) {
                  exactMatch.first()
               } else {
                  val nonNullMatches = matches.filter { it.value != null }
                  if (nonNullMatches.size == 1) {
                     nonNullMatches.first()
                  } else {
                     log().debug(
                        "ANY_DEPTH_EXPECT_ONE strategy found {} of type {}, so returning null",
                        matches.size,
                        type.name
                     )
                     null
                  }
               }
            }
         }
      }
   },

   /**
    * Will return matches from any depth, providing there is exactly
    * one DISITNCT match within the context
    */
   ANY_DEPTH_ALLOW_MANY {
      override fun getFact(
         context: QueryContext,
         type: Type,
         matcher: TypeMatchingStrategy,
         spec: TypedInstanceValidPredicate
      ): TypedCollection? {
         val matches = context.modelTree()
            .filter { matcher.matches(type, it.type) }
            .filter { spec.isValid(it) }
            .distinct()
            .toList()
         return when {
            matches.isEmpty() -> null
            else -> TypedCollection.from(matches)
         }
      }
   },

   ANY_DEPTH_ALLOW_MANY_UNWRAP_COLLECTION {
      override fun getFact(
         context: QueryContext,
         type: Type,
         matcher: TypeMatchingStrategy,
         spec: TypedInstanceValidPredicate
      ): TypedCollection? {
         val matches = context.modelTree()
            .filter { matcher.matches(if (type.isCollection) type.typeParameters.first() else type, it.type) }
            .filter { spec.isValid(it) }
            .distinct()
            .toList()
         return when {
            matches.isEmpty() -> null
            else -> TypedCollection.from(matches)
         }
      }
   };


   abstract fun getFact(
      context: QueryContext,
      type: Type,
      strictness: TypeMatchingStrategy = TypeMatchingStrategy.ALLOW_INHERITED_TYPES,
      spec: TypedInstanceValidPredicate
   ): TypedInstance?

}


fun <K, V> HashMultimap<K, V>.copy(): HashMultimap<K, V> {
   return HashMultimap.create(this)
}
