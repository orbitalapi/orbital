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
import io.vyne.query.graph.*
import io.vyne.schemas.*
import io.vyne.utils.ImmutableEquality
import io.vyne.utils.cached
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import lang.taxi.policies.Instruction
import lang.taxi.types.EnumType
import lang.taxi.types.PrimitiveType
import lang.taxi.types.ProjectedType
import mu.KotlinLogging
import java.util.concurrent.CopyOnWriteArrayList
import java.util.stream.Stream
import kotlin.streams.toList

private val logger = KotlinLogging.logger {}

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
   @Deprecated("Not used, not required")
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
   val querySpec: QuerySpecTypeNode,
   @field:JsonIgnore // we send a lightweight version below
   val results: Flow<TypedInstance>,
   @Deprecated("Being removed, QueryResult is now just a wrapper around the results")
   @field:JsonIgnore // this sends too much information - need to build a lightweight version
   override val profilerOperation: ProfilerOperation? = null,
   @Deprecated("It's no longer possible to know at the time the QueryResult is intantiated if the query has been fully resolved.  Catch the exception from the Flow<> instead.")
   override val isFullyResolved: Boolean,
   val anonymousTypes: Set<Type> = setOf(),
   override val clientQueryId: String? = null,
   override val queryId: String
) : QueryResponse {
   override val queryResponseId: String = queryId
   val duration = profilerOperation?.duration

   @Deprecated(
      "Now that a query only reflects a single type, this does not make sense anymore",
      replaceWith = ReplaceWith("isFullyResolved")
   )
   @get:JsonIgnore // Deprecated
   val unmatchedNodes: Set<QuerySpecTypeNode> by lazy {
      setOf(querySpec)
   }
   override val responseStatus: ResponseStatus = if (isFullyResolved) COMPLETED else INCOMPLETE

   // for UI
   val searchedTypeName: QualifiedName = querySpec.type.qualifiedName

   /**
    * Returns the result stream with all type information removed.
    */
   @get:JsonIgnore
   val rawResults: Flow<Any?>
      get() {
         val converter = TypedInstanceConverter(RawObjectMapper)
         return results.map {
            converter.convert(it)
         }
      }

   /**
    * Returns the result stream converted to TypeNamedInstances.
    * Note that depending on the actual values provided in the results,
    * we may emit TypeNamedInstance or TypeNamedInstace[].  Nulls
    * present in the result stream are not filtered.
    * For these reasons, the result is Flow<Any?>
    *
    */
   @get:JsonIgnore
   val typedNamedInstanceResults: Flow<Any?>
      get() {
         val converter = TypedInstanceConverter(TypeNamedInstanceMapper)
         return results.map { converter.convert(it) }
      }
}

// Note : Also models failures, so is fairly generic
interface QueryResponse {
   enum class ResponseStatus {
      UNKNOWN,
      COMPLETED,
      RUNNING,

      // Ie., the query didn't error, but not everything was resolved
      INCOMPLETE,
      ERROR,
   }

   val responseStatus: ResponseStatus
   val queryResponseId: String
   val clientQueryId: String?
   val queryId: String

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

}

fun collateRemoteCalls(profilerOperation: ProfilerOperation?): List<RemoteCall> {
   if (profilerOperation == null) return emptyList()
   return profilerOperation.remoteCalls + profilerOperation.children.flatMap { collateRemoteCalls(it) }
}

object TypedInstanceTree {
   /**
    * Function which defines how to convert a TypedInstance into a tree, for traversal
    */

   fun visit(instance: TypedInstance): List<TypedInstance> {

      if (instance.type.isClosed) {
         return emptyList()
      }

      return when (instance) {
         is TypedObject -> instance.values.toList()
         is TypedEnumValue -> {
            instance.synonyms
         }
         is TypedValue -> {
            if (instance.type.isEnum) {
               TODO("Why are we hitting this?")
               EnumSynonyms.fromTypeValue(instance)
            } else {
               emptyList()
            }

         }
         is TypedCollection -> instance.value
         is TypedNull -> emptyList()
         else -> throw IllegalStateException("TypedInstance of type ${instance.javaClass.simpleName} is not handled")
      }
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
   val facts: CopyOnWriteArrayList<TypedInstance>,
   val queryEngine: QueryEngine,
   val profiler: QueryProfiler,
   val debugProfiling: Boolean = false,
   val parent: QueryContext? = null,
   /**
    * A user supplied id they can use to reference this query.
    * Note that the REAL id for a query is the one used in query result,
    * however we allow clients to provide their own ids.
    * We don't really care about clashes at this point, but may
    * protect against it at a later time.
    */
   val clientQueryId: String? = null,
   /**
    * Unique ID generated for query context
    */
   val queryId: String,

   val eventBroker: QueryContextEventBroker = QueryContextEventBroker()

) : ProfilerOperation by profiler, QueryContextEventDispatcher {

   private val evaluatedEdges = mutableListOf<EvaluatedEdge>()
   private val policyInstructionCounts = mutableMapOf<Pair<QualifiedName, Instruction>, Int>()
   var isProjecting = false
   var projectResultsTo: Type? = null
      private set;

   var cancelRequested: Boolean = false
      private set;

   init {
      val self = this
      eventBroker.addHandler(object : CancelRequestHandler {
         override fun requestCancel() {
            self.requestCancel()
         }
      })
   }

   override fun requestCancel() {
      logger.info { "Cancelling query $queryId" }
      cancelRequested = true
   }


   override fun reportIncrementalEstimatedRecordCount(operation: RemoteOperation, estimatedRecordCount: Int) {
      if (this.isProjecting) {
         logger.debug { "Not reporting incremental estimated record count, as currently projecting" }
      } else {
         this.eventBroker.reportIncrementalEstimatedRecordCount(operation, estimatedRecordCount)
      }
   }


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
         profiler: QueryProfiler,
         clientQueryId: String? = null,
         queryId: String,
         eventBroker: QueryContextEventBroker = QueryContextEventBroker()
      ): QueryContext {
         return QueryContext(
            schema,
            CopyOnWriteArrayList(facts),
            queryEngine,
            profiler,
            clientQueryId = clientQueryId,
            queryId = queryId,
            eventBroker = eventBroker
         )
      }

   }

   private fun resolveSynonyms(fact: TypedInstance, schema: Schema): Set<TypedInstance> {
      return if (fact is TypedObject) {
         fact.values.flatMap { resolveSynonym(it, schema, false).toList() }.toSet().plus(fact)
      } else {
         resolveSynonym(fact, schema, true)
      }
   }

   private fun resolveSynonym(fact: TypedInstance, schema: Schema, includeGivenFact: Boolean): Set<TypedInstance> {
      val derivedFacts = if (fact.type.isEnum && fact.value != null) {
         val underlyingEnumType = fact.type.taxiType as EnumType
         underlyingEnumType.of(fact.value)
            .synonyms
            .map { synonym ->
               val synonymType = schema.type(synonym.synonymFullyQualifiedName())
               val synonymTypeTaxiType = synonymType.taxiType as EnumType
               val synonymEnumValue = synonymTypeTaxiType.of(synonym.synonymValue())

               // Instantiate with either name or value depending on what we have as input
               val value =
                  if (underlyingEnumType.hasValue(fact.value)) synonymEnumValue.value else synonymEnumValue.name

               TypedValue.from(
                  synonymType,
                  value,
                  false,
                  MappedSynonym(fact)
               )
            }.toSet()
      } else {
         setOf()
      }

      return if (includeGivenFact) {
         derivedFacts.plus(fact)
      } else {
         derivedFacts
      }
   }

   /**
    * Returns a QueryContext, with only the provided fact.
    * All other parameters (queryEngine, schema, etc) are retained
    */
   fun only(fact: TypedInstance): QueryContext {

      val mutableFacts = CopyOnWriteArrayList(listOf(fact))
      val copied = this.copy(facts = mutableFacts, parent = this)
      copied.excludedOperations.addAll(this.schema.excludedOperationsForEnrichment())
      copied.excludedServices.addAll(this.excludedServices)
      return copied
   }

   fun addFact(fact: TypedInstance): QueryContext {
      this.facts.add(fact)
      this.modelTree.invalidate()
      // Now that we have a new fact, invalidate queries where we had asked for a fact
      // previously, and had returned null.
      // This allows new queries to discover new values.
      // All other getFactOrNull() calls will retain cached values.
      this.factSearchCache.removeValues { _, typedInstance -> typedInstance == null }
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
      return projectResultsTo(ProjectedType.fromConcreteTypeOnly(schema.taxi.type(targetType)))
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

   private val modelTree = cached<List<TypedInstance>> {
      val navigator = TreeNavigator()
      val treeDef: TreeDef<TypedInstance> = TreeDef.of { instance -> navigator.visit(instance) }
      TreeStream.breadthFirst(treeDef, dataTreeRoot()).toList()
   }

   /**
    * A breadth-first stream of data facts currently held in the collection.
    * Use breadth-first, as we want to favour nodes closer to the root.
    * Deeply nested children are less likely to be relevant matches.
    */
   fun modelTree(): Stream<TypedInstance> {
      // TODO : MP - Investigating the performance implications of caching the tree.
      // If this turns out to be faster, we should refactor the api to be List<TypedInstance>, since
      // the stream indicates deferred evaluation, and it's not anymore.
      return modelTree.get().stream()
   }

   private data class GetFactOrNullCacheKey(
      val search: ContextFactSearch
   ) {
      private val equality = ImmutableEquality(
         this,
         GetFactOrNullCacheKey::search,
      )

      override fun equals(other: Any?): Boolean {
         return equality.isEqualTo(other)
      }

      override fun hashCode(): Int {
         return equality.hash()
      }
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


   /**
    * getFactOrNull is called frequently, and can generate a VERY LARGE call stack.  In some profiler passes, we've
    * seen 40k calls to getFactOrNull, which in turn generates a call stack with over 18M invocations.
    * So, cache the calls.
    */
   private val factSearchCache = cached { key: GetFactOrNullCacheKey ->
      key.search.strategy.getFact(this, key.search)
   }

   fun getFactOrNull(
      type: Type,
      strategy: FactDiscoveryStrategy = TOP_LEVEL_ONLY,
      spec: TypedInstanceValidPredicate = AlwaysGoodSpec
   ): TypedInstance? {
      return factSearchCache.get(GetFactOrNullCacheKey(ContextFactSearch.findType(type, strategy, spec)))
   }

   fun getFactOrNull(
      search: ContextFactSearch,
   ): TypedInstance? {
      return factSearchCache.get(GetFactOrNullCacheKey(search))
   }

   fun hasFact(
      search: ContextFactSearch
   ): Boolean {
      return getFactOrNull(search) != null
   }


   fun evaluatedPath(): List<EvaluatedEdge> {
      return evaluatedEdges.toList()
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
      logger.debug { "Caching $operation [${operation.previousValue?.value} -> ${result.type.qualifiedName}]" }
      return result
   }

   fun onServiceInvoked(invokedService: Service?) {
      if (invokedService?.hasMetadata(ServiceAnnotations.Datasource.annotation) == true) {
         // This is a work-around to a search limitation.
         // Currently, Vyne will attempt to discover from any service that returns the output expected.
         // We should limit, such that if an entity decalres an Id, then we should only invoke that service if the
         // @Id is known to us.
         // We expect to remove this once search-only-on-id is completed.
         invokedService.metadata(ServiceAnnotations.Datasource.annotation).params[ServiceParams.Exclude.paramName]?.let { excludedServiceList ->
            //TODO check taxi annotation param value schema generation.
            // as currently the value of 'excluded' is a list of string
            // but it comes as a string in the form of [[service1, service2]]
            val excludedServiceString: String? = excludedServiceList as? String
            excludedServiceString?.let { serviceList ->
               serviceList.filter { it != '[' && it != ']' }
            }.toString()
               .split(",").forEach { serviceToExclude ->
                  excludedServices.add(
                     SearchGraphExclusion(
                        "Exclude already invoked @DataSource annotated services from discovery searches",
                        QualifiedName(serviceToExclude)
                     )
                  )
               }
         }
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


fun <K, V> HashMultimap<K, V>.copy(): HashMultimap<K, V> {
   return HashMultimap.create(this)
}

class TreeNavigator {
   private val visitedNodes = mutableSetOf<TypedInstance>()

   fun visit(instance: TypedInstance): List<TypedInstance> {
      return if (visitedNodes.contains(instance)) {
         return emptyList()
      } else {
         visitedNodes.add(instance)
         TypedInstanceTree.visit(instance)
      }
   }
}

/**
 * Lightweight interface to allow components used throughout execution of a query
 * to send messages back up to the QueryContext.
 *
 * It's up to the query context what to do with these messages.  It may ignore them,
 * or redistribute them.  Callers should not make any assumptions about the impact of calling these methods.
 *
 * Using an interface here as we don't always actually have a query context.
 */
interface QueryContextEventDispatcher {
   /**
    * Signals an incremental update to the estimated record count, as reported by the provided operation.
    * This is populated by services setting the HttpHeaders.STREAM_ESTIMATED_RECORD_COUNT header
    * in their response to Vyne.
    */
   fun reportIncrementalEstimatedRecordCount(operation: RemoteOperation, estimatedRecordCount: Int) {}

   /**
    * Request that this query cancel.
    */
   fun requestCancel() {}
}


/**
 * Ok, here's the deal... this EventDispatcher / Broker stuff doesn't feel right.
 * I'm like...three wines deep, and three weeks late in shipping this f**ing release.
 * It'll do, ok?
 */
class QueryContextEventBroker : QueryContextEventDispatcher {
   private val handlers = mutableListOf<QueryContextEventHandler>()
   fun addHandler(handler: QueryContextEventHandler): QueryContextEventBroker {
      handlers.add(handler)
      return this
   }

   override fun reportIncrementalEstimatedRecordCount(operation: RemoteOperation, estimatedRecordCount: Int) {
      handlers.filterIsInstance<EstimatedRecordCountUpdateHandler>()
         .forEach { it.reportIncrementalEstimatedRecordCount(operation, estimatedRecordCount) }
   }

   override fun requestCancel() {
      handlers.filterIsInstance<CancelRequestHandler>()
         .forEach { it.requestCancel() }
   }

}

interface QueryContextEventHandler
interface EstimatedRecordCountUpdateHandler : QueryContextEventHandler {
   fun reportIncrementalEstimatedRecordCount(operation: RemoteOperation, estimatedRecordCount: Int) {}
}

interface CancelRequestHandler : QueryContextEventHandler {
   fun requestCancel() {}
}

object NoOpQueryContextEventDispatcher : QueryContextEventDispatcher


