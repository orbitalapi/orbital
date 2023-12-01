package com.orbitalhq.query

import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.KeyDeserializer
import com.google.common.collect.HashMultimap
import com.orbitalhq.FactSets
import com.orbitalhq.metrics.NoOpMetricsReporter
import com.orbitalhq.metrics.QueryMetricsReporter
import com.orbitalhq.retainFactsFromFactSet
import com.orbitalhq.models.*
import com.orbitalhq.models.facts.*
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.query.graph.ServiceAnnotations
import com.orbitalhq.query.graph.ServiceParams
import com.orbitalhq.query.graph.edges.EvaluatableEdge
import com.orbitalhq.query.graph.edges.EvaluatedEdge
import com.orbitalhq.schemas.*
import com.orbitalhq.utils.Ids
import com.orbitalhq.utils.StrategyPerformanceProfiler
import com.orbitalhq.utils.orElse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.merge
import lang.taxi.accessors.ProjectionFunctionScope
import lang.taxi.policies.Instruction
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList


class QueryResultResultsAttributeKeyDeserialiser : KeyDeserializer() {
   override fun deserializeKey(p0: String?, p1: DeserializationContext?): Any? {
      return null
   }

}

fun collateRemoteCalls(profilerOperation: ProfilerOperation?): List<RemoteCall> {
   if (profilerOperation == null) return emptyList()
   return profilerOperation.remoteCalls + profilerOperation.children.flatMap { collateRemoteCalls(it) }
}

enum class FailureBehaviour {

   /**
    * Throwing is the original behaviour. However, this becomes destructive
    * when throwing inside a mapping operation, as it kills the other
    * active mapping operations.
    */
   THROW,

   /**
    * If a query fails, send a typed null, with a DataSource of
    * FailedSearch.
    *
    * This is less destructive than throwing an exception, but
    * can be ambiguous for consumers.
    */
   SEND_TYPED_NULL;
}

object QueryCancellationRequest
// Design choice:
// Query Context's don't have a concept of FactSets, everything is just flattened to facts.
// However, the QueryEngineFactory DOES retain the concept.
// This is because by the time you go to run a query, you should be focussed on
// "What do I know", and not "Where did I learn this?"
// At one point, the FactSetMap leaked down to QueryContext and beyond, and this caused
// many different classes to have to become aware of multiple factsets, which felt like a leak.
// Revisit if the above becomes less true.

data class QueryContext(
   override val schema: Schema,

   // TODO : Facts should become "scoped", to allow us to carefully
   // manage which facts are shared between contexts when doing things like
   // projecting.
   // CascadingFactBag will be useful here.
   val facts: FactBag,
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

   val eventBroker: QueryContextEventBroker = QueryContextEventBroker(),

   val functionResultCache: MutableMap<FunctionResultCacheKey, Any> = ConcurrentHashMap(),

   val queryOptions: QueryOptions,

//   val failureBehaviour: FailureBehaviour = FailureBehaviour.THROW
   val metricsReporter: QueryMetricsReporter = NoOpMetricsReporter


) : ProfilerOperation by profiler, FactBag by facts, QueryContextEventDispatcher by eventBroker, InPlaceQueryEngine, QueryContextSchemaProvider {

   private val logger = KotlinLogging.logger {}
   private val evaluatedEdges = mutableListOf<EvaluatedEdge>()
   private val policyInstructionCounts = mutableMapOf<Pair<QualifiedName, Instruction>, Int>()
   var isProjecting = false
//   var projectResultsTo: Type? = null
//      private set

   var projectionScope: ProjectionFunctionScope? = null
      private set

   var responseType: String? = null
      private set

   private val cancelEmitter = Sinks.many().multicast().onBackpressureBuffer<QueryCancellationRequest>()
   val cancelFlux: Flux<QueryCancellationRequest> = cancelEmitter.asFlux()
   private var isCancelRequested: Boolean = false

   override fun requestCancel() {
      logger.info { "Cancelling query $queryId" }
      cancelEmitter.tryEmitNext(QueryCancellationRequest)
      isCancelRequested = true
   }

   val cancelRequested: Boolean
      get() {
         return isCancelRequested || parent?.cancelRequested.orElse(false)
      }


   override fun reportIncrementalEstimatedRecordCount(operation: RemoteOperation, estimatedRecordCount: Int) {
      if (this.isProjecting) {
         logger.debug { "Not reporting incremental estimated record count, as currently projecting" }
      } else {
         this.eventBroker.reportIncrementalEstimatedRecordCount(operation, estimatedRecordCount)
      }
   }

   override fun addFact(fact: TypedInstance): QueryContext {
      facts.addFact(fact)
      return this
   }


   override fun toString() = "# of facts=${facts.size} #schema types=${schema.types.size}"
   suspend fun find(
      typeName: String,
      permittedStrategy: PermittedQueryStrategies = PermittedQueryStrategies.EVERYTHING,
      metricsTags: MetricTags = MetricTags.NONE
   ): QueryResult = find(TypeNameQueryExpression(typeName), permittedStrategy, metricsTags = metricsTags)

   suspend fun find(
      queryString: QueryExpression,
      permittedStrategy: PermittedQueryStrategies = PermittedQueryStrategies.EVERYTHING,
      failureBehaviour: FailureBehaviour = FailureBehaviour.THROW,
      metricsTags: MetricTags = MetricTags.NONE
   ): QueryResult = queryEngine.find(
      queryString,
      this.newSearchContext(),
      applicableStrategiesPredicate = PermittedQueryStrategyPredicate.forEnum(permittedStrategy),
      failureBehaviour = failureBehaviour,
      metricsTags = metricsTags
   )

   suspend fun find(target: QuerySpecTypeNode, failureBehaviour: FailureBehaviour = FailureBehaviour.THROW,
                    metricsTags: MetricTags = MetricTags.NONE): QueryResult = queryEngine.find(target, this.newSearchContext(), failureBehaviour = failureBehaviour, metricsTags = metricsTags)
   suspend fun find(target: Set<QuerySpecTypeNode>, failureBehaviour: FailureBehaviour = FailureBehaviour.THROW,
                    metricsTags: MetricTags = MetricTags.NONE): QueryResult = queryEngine.find(target, this.newSearchContext(), failureBehaviour = failureBehaviour, metricsTags = metricsTags)
   suspend fun find(
      target: QuerySpecTypeNode,
      excludedOperations: Set<SearchGraphExclusion<RemoteOperation>>,
      failureBehaviour:FailureBehaviour = FailureBehaviour.THROW,
      metricsTags: MetricTags = MetricTags.NONE
   ): QueryResult =
      queryEngine.find(target, this.newSearchContext(), excludedOperations, failureBehaviour = failureBehaviour, metricsTags = metricsTags)

   suspend fun build(type: Type):QueryResult = build(TypeQueryExpression(type))
   suspend fun build(typeName: QualifiedName): QueryResult = build(typeName.parameterizedName)
   suspend fun build(typeName: String): QueryResult =
      queryEngine.build(TypeNameQueryExpression(typeName), this.newSearchContext())

   suspend fun build(expression: QueryExpression, metricsTags: MetricTags = MetricTags.NONE): QueryResult =
      //timed("QueryContext.build") {
      queryEngine.build(expression, this.newSearchContext())
   //}

   suspend fun findAll(typeName: String,
                       metricsTags: MetricTags = MetricTags.NONE): QueryResult = findAll(TypeNameQueryExpression(typeName), metricsTags)
   suspend fun findAll(queryString: QueryExpression,
                       metricsTags: MetricTags = MetricTags.NONE): QueryResult =
      queryEngine.findAll(queryString, this.newSearchContext(), metricsTags)

   suspend fun doMap(expression: QueryExpression, metricsTags : MetricTags = MetricTags.NONE): QueryResult {
      val querySpec = queryEngine.parse(expression)
      val sourceFacts = when {
         // Don't use .isEmpty(), as it also considers scoped facts
         @Suppress("ReplaceSizeZeroCheckWithIsEmpty")
         this.facts.size != 0 -> this.facts // given { ... }
         this.scopedFacts.isNotEmpty() -> this.scopedFacts.map { it.fact } // query foo( @RequestBody input:T[] ) ....
         else -> error("When calling map {}, exactly one input fact is expected, but none were found.")
      }
      require(sourceFacts.size == 1) { "When calling map { }, exactly one input fact is expected, but found ${sourceFacts.size}" }
      val sourceCollection = sourceFacts.single()
      require(sourceCollection is TypedCollection) { "When calling map {}, the input fact is expected to be a collection.  Instead, found ${sourceCollection.type.paramaterizedName}" }
      val mappingResult = sourceCollection.map { inputValue ->
         try {
            // Do not include the source collection,
            // as that can break things where we expect to find a single result.
            val scopedFactsWithoutInput = this.scopedFacts.filter { it.fact != sourceCollection }


            val mappingQueryContext = this.only(inputValue, scopedFactsWithoutInput)
               .copy(
                  parent = null,
               )

            val mappingQueryEngine = queryEngine.newEngine { existingFacts ->
               // Only retain user info, drop all other facts (like initial state)
               val newState = existingFacts.retainFactsFromFactSet(setOf(FactSets.CALLER))
               newState.put(FactSets.DEFAULT, inputValue)
               newState
            }
            // Don't throw errors inside a mapping operation,
            // as it takes down the other maps that are running
            mappingQueryEngine.find(
               expression,
               mappingQueryContext,
               failureBehaviour = FailureBehaviour.SEND_TYPED_NULL
            )
         } catch (e: Exception) {
            QueryResult(
               querySpec.first(),
               emptyFlow(),
               null,
               false,
               emptySet(),
               clientQueryId, queryId,
               schema = schema
            )

         }


      }
      // HACK: Just reusing the first result, but rewiring the result, and a few params
      // like ids, etc.
      return if (mappingResult.isEmpty()) {
         QueryResult(
            querySpec.first(),
            emptyFlow(),
            null,
            false,
            emptySet(),
            clientQueryId, queryId,
            schema = schema
         )
      } else {
         val firstResult = mappingResult.first()
         val combinedResultsFlow = mappingResult.map { it.results }.merge()
         firstResult.copy(results = combinedResultsFlow, queryId = this.queryId, clientQueryId = this.clientQueryId)
      }
   }


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
         eventBroker: QueryContextEventBroker = QueryContextEventBroker(),
         scopedFacts: List<ScopedFact> = emptyList(),
         queryOptions: QueryOptions,
         metricsReporter: QueryMetricsReporter = NoOpMetricsReporter
      ): QueryContext {
         return QueryContext(
            schema,
            CopyOnWriteFactBag(facts, schema, scopedFacts),
            queryEngine,
            profiler,
            clientQueryId = clientQueryId,
            queryId = queryId,
            eventBroker = eventBroker,
            queryOptions =  queryOptions,
            metricsReporter = metricsReporter
         )
      }
   }

   private fun newSearchContext(clientQueryId: String? = Ids.id(prefix = "clientQueryId", size = 8)): QueryContext {
      // WTF: Found this. We're not actually creating a new instance here.
      // Don't wanna change this right now, and I know this is my own doing, but
      // WTF was I thinking?
      // When fixing this in the future, consider that when calling .map {} (handled in doMap()),
      // we want to reuse the same queryId and clientQueryId, as it's the same query.
      return this

      val clone = this.copy(
         clientQueryId = clientQueryId,
         queryId = Ids.id("query")
      )
      clone.projectionScope = null
      return clone
   }

   /**
    * Returns a QueryContext, with only the provided fact.
    * All other parameters (queryEngine, schema, etc) are retained
    */
   override fun only(fact: TypedInstance, scopedFacts: List<ScopedFact>, inheritParent: Boolean): QueryContext {
      return only(listOf(fact), scopedFacts)
   }

   override fun only(facts: List<TypedInstance>, scopedFacts: List<ScopedFact>, inheritParent: Boolean): QueryContext {
      val parent = if (inheritParent) this else null
      val copied = this.newSearchContext().copy(
         facts = CopyOnWriteFactBag(CopyOnWriteArrayList(facts), scopedFacts, schema),
         parent = parent,
      )
      appendExclusionsToContext(copied)
      return copied
   }

   fun only(): QueryContext {
      val copied = this.newSearchContext()
      appendExclusionsToContext(copied)
      return copied
   }

   private fun appendExclusionsToContext(context: QueryContext) {
      context.excludedOperations.addAll(this.schema.excludedOperationsForEnrichment())
      context.excludedServices.addAll(this.excludedServices)
   }

   /**
    * Returns a QueryContext, with all of the current facts, plus the additional fact and scope
    * appended.
    *
    * All other parameters (queryEngine, schema and excluded operations) are retained.
    * The current queryContext is not affected by mutations in the new queryContext
    */
   override fun withAdditionalFacts(facts: List<TypedInstance>, scopedFacts: List<ScopedFact>): QueryContext {
      val additionalFacts = CopyOnWriteFactBag(CopyOnWriteArrayList(facts), scopedFacts, schema)
      val copied = this.newSearchContext().copy(
         facts = CascadingFactBag(additionalFacts, this.facts),
         parent = this,
      )
      appendExclusionsToContext(copied)
      return copied
   }


   fun responseType(responseType: String?): QueryContext {
      this.responseType = responseType
      return this
   }


   override suspend fun findType(type: Type, permittedStrategy: PermittedQueryStrategies): Flow<TypedInstance> {
      return this.find(TypeQueryExpression(type), permittedStrategy)
         .results
   }

   fun evaluatedPath(): List<EvaluatedEdge> {
      return evaluatedEdges.toList()
   }


   fun addAppliedInstruction(policy: Policy, instruction: Instruction) {
      policyInstructionCounts.compute(policy.name to instruction) { _, atomicInteger -> if (atomicInteger != null) atomicInteger + 1 else 1 }
   }


   data class FactCacheKey(val fqn: String, val discoveryStrategy: FactDiscoveryStrategy)
   data class ServiceInvocationCacheKey(
      private val operationName: String, // Use String, rather than QualifiedName to prevent too much object creation
      private val invocationParameter: Set<TypedInstance?>
   )

   private val operationCache: MutableMap<ServiceInvocationCacheKey, TypedInstance> = mutableMapOf()
   val excludedServices: MutableSet<SearchGraphExclusion<QualifiedName>> = mutableSetOf()
   val excludedOperations: MutableSet<RemoteOperation> = mutableSetOf()


   private fun getTopLevelContext(): QueryContext {
      return parent?.getTopLevelContext() ?: this
   }

   fun notifyOperationResult(
      operation: EvaluatableEdge,
      result: TypedInstance,
      callArgs: Set<TypedInstance?>,
      addToOperationResultCache: Boolean = true
   ): TypedInstance {
      return notifyOperationResult(operation.vertex1.value.toString(), result, callArgs, addToOperationResultCache)
   }

   fun notifyOperationResult(
      operationName: String,
      result: TypedInstance,
      callArgs: Set<TypedInstance?>,
      addToOperationResultCache: Boolean = true
   ): TypedInstance {
      val (service, _) = OperationNames.serviceAndOperation(operationName)

      val invokedService = schema.service(service)
      onServiceInvoked((invokedService))

      if (addToOperationResultCache) {
         val cacheKey = ServiceInvocationCacheKey(operationName, callArgs)
         getTopLevelContext().operationCache[cacheKey] = result
         logger.debug { "Caching $operationName -> ${result.type.qualifiedName}]" }
      }
      return result
   }

   fun notifyOperationResult(
      operationResult: OperationResult
   ) {
      eventBroker.reportRemoteOperationInvoked(operationResult, this.queryId)
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
                        QualifiedName.from(serviceToExclude)
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

   fun getOperationResult(operationName: String, callArgs: Set<TypedInstance?>): TypedInstance? {
      val key = ServiceInvocationCacheKey(operationName, callArgs)
      return getTopLevelContext().operationCache[key]
   }

   fun hasOperationResult(operationName: String, callArgs: Set<TypedInstance?>): Boolean {
      val key = ServiceInvocationCacheKey(operationName, callArgs)
      return getTopLevelContext().operationCache[key] != null
   }

   suspend fun invokeOperation(
      operationName: QualifiedName,
      preferredParams: Set<TypedInstance> = emptySet(),
      providedParamValues: List<Pair<Parameter, TypedInstance>> = emptyList()
   ): Flow<TypedInstance> {
      val (service, operation) = this.schema.operation(operationName)
      return invokeOperation(service, operation)
   }

   suspend fun invokeOperation(
      service: Service,
      operation: Operation,
      preferredParams: Set<TypedInstance> = emptySet(),
      providedParamValues: List<Pair<Parameter, TypedInstance>> = emptyList()
   ): Flow<TypedInstance> {
      return queryEngine.invokeOperation(
         service, operation, preferredParams, this, providedParamValues
      )
   }

   /**
    * Call this function at the mutate phase of the query execution.
    */
   suspend fun mutate(expression: MutatingQueryExpression, metricsTags: MetricTags = MetricTags.NONE): QueryResult {
      val querySpec = parseQuery(expression).single()
      return queryEngine.mutate(expression.mutation!!, querySpec, this, inputValue = null, metricsTags = metricsTags)

   }
}


fun <K, V> HashMultimap<K, V>.copy(): HashMultimap<K, V> {
   return HashMultimap.create(this)
}


/**
 * Ok, here's the deal... this EventDispatcher / Broker stuff doesn't feel right.
 * I'm like...three wines deep, and three weeks late in shipping this f**ing release.
 * It'll do, ok?
 */
class QueryContextEventBroker : QueryContextEventDispatcher {
   private val handlers = CopyOnWriteArrayList<QueryContextEventHandler>()

   fun addHandler(handler: QueryContextEventHandler): QueryContextEventBroker {
      handlers.add(handler)
      return this
   }

   fun addHandlers(handlers: List<QueryContextEventHandler>): QueryContextEventBroker {
      this.handlers.addAll(handlers)
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

   override fun reportRemoteOperationInvoked(operation: OperationResult, queryId: String) {
      StrategyPerformanceProfiler.profiled("reportRemoteOperationInvoked") {
         handlers.filterIsInstance<RemoteCallOperationResultHandler>()
            .forEach { it.recordResult(operation, queryId) }
      }

   }

}

interface QueryContextEventHandler
interface EstimatedRecordCountUpdateHandler : QueryContextEventHandler {
   fun reportIncrementalEstimatedRecordCount(operation: RemoteOperation, estimatedRecordCount: Int) {}
}

interface CancelRequestHandler : QueryContextEventHandler {
   fun requestCancel() {}
}

object NoOpQueryContextEventDispatcher : QueryContextEventDispatcher {
   override fun reportIncrementalEstimatedRecordCount(operation: RemoteOperation, estimatedRecordCount: Int) {
   }

   override fun requestCancel() {
   }

   override fun reportRemoteOperationInvoked(operation: OperationResult, queryId: String) {
   }
}

interface RemoteCallOperationResultHandler : QueryContextEventHandler {
   fun recordResult(operation: OperationResult, queryId: String)
}
