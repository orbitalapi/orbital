package io.vyne.query

import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.KeyDeserializer
import com.google.common.collect.HashMultimap
import io.vyne.models.InPlaceQueryEngine
import io.vyne.models.OperationResult
import io.vyne.models.TypedInstance
import io.vyne.models.facts.CopyOnWriteFactBag
import io.vyne.models.facts.FactBag
import io.vyne.models.facts.FactDiscoveryStrategy
import io.vyne.models.facts.ScopedFact
import io.vyne.models.functions.FunctionResultCacheKey
import io.vyne.query.graph.ServiceAnnotations
import io.vyne.query.graph.ServiceParams
import io.vyne.query.graph.edges.EvaluatableEdge
import io.vyne.query.graph.edges.EvaluatedEdge
import io.vyne.schemas.*
import io.vyne.utils.Ids
import io.vyne.utils.StrategyPerformanceProfiler
import io.vyne.utils.orElse
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import lang.taxi.accessors.ProjectionFunctionScope
import lang.taxi.policies.Instruction
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger


class QueryResultResultsAttributeKeyDeserialiser : KeyDeserializer() {
   override fun deserializeKey(p0: String?, p1: DeserializationContext?): Any? {
      return null
   }

}

fun collateRemoteCalls(profilerOperation: ProfilerOperation?): List<RemoteCall> {
   if (profilerOperation == null) return emptyList()
   return profilerOperation.remoteCalls + profilerOperation.children.flatMap { collateRemoteCalls(it) }
}

data class VyneQueryStatistics(
   val graphCreatedCount: AtomicInteger = AtomicInteger(0),
   val graphSearchSuccessCount: AtomicInteger = AtomicInteger(0),
   val graphSearchFailedCount: AtomicInteger = AtomicInteger(0)
) {
   companion object {
      fun from(serializableVyneQueryStatistics: SerializableVyneQueryStatistics): VyneQueryStatistics {
         return VyneQueryStatistics(
            graphCreatedCount = AtomicInteger(serializableVyneQueryStatistics.graphCreatedCount),
            graphSearchSuccessCount = AtomicInteger(serializableVyneQueryStatistics.graphSearchSuccessCount),
            graphSearchFailedCount = AtomicInteger(serializableVyneQueryStatistics.graphSearchFailedCount)
         )
      }
   }
}

@Serializable
data class SerializableVyneQueryStatistics(
   val graphCreatedCount: Int,
   val graphSearchSuccessCount: Int,
   val graphSearchFailedCount: Int
) {
   companion object {
      fun from(vyneQueryStatistics: VyneQueryStatistics): SerializableVyneQueryStatistics {
         return SerializableVyneQueryStatistics(
            graphCreatedCount = vyneQueryStatistics.graphCreatedCount.get(),
            graphSearchSuccessCount = vyneQueryStatistics.graphSearchSuccessCount.get(),
            graphSearchFailedCount = vyneQueryStatistics.graphSearchFailedCount.get(),
         )
      }
   }
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
   val schema: Schema,

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

   val vyneQueryStatistics: VyneQueryStatistics = VyneQueryStatistics(),

   val functionResultCache: MutableMap<FunctionResultCacheKey, Any> = ConcurrentHashMap()

   ) : ProfilerOperation by profiler, FactBag by facts, QueryContextEventDispatcher, InPlaceQueryEngine {

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
   suspend fun find(typeName: String): QueryResult = find(TypeNameQueryExpression(typeName))

   suspend fun find(queryString: QueryExpression): QueryResult = queryEngine.find(queryString, this.newSearchContext())
   suspend fun find(target: QuerySpecTypeNode): QueryResult = queryEngine.find(target, this.newSearchContext())
   suspend fun find(target: Set<QuerySpecTypeNode>): QueryResult = queryEngine.find(target, this.newSearchContext())
   suspend fun find(target: QuerySpecTypeNode, excludedOperations: Set<SearchGraphExclusion<RemoteOperation>>): QueryResult =
      queryEngine.find(target, this.newSearchContext(), excludedOperations)

   suspend fun build(typeName: QualifiedName): QueryResult = build(typeName.parameterizedName)
   suspend fun build(typeName: String): QueryResult = queryEngine.build(TypeNameQueryExpression(typeName), this.newSearchContext())
   suspend fun build(expression: QueryExpression): QueryResult =
      //timed("QueryContext.build") {
      queryEngine.build(expression, this.newSearchContext())
   //}

   suspend fun findAll(typeName: String): QueryResult = findAll(TypeNameQueryExpression(typeName))
   suspend fun findAll(queryString: QueryExpression): QueryResult = queryEngine.findAll(queryString, this.newSearchContext())

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
            CopyOnWriteFactBag(facts, schema),
            queryEngine,
            profiler,
            clientQueryId = clientQueryId,
            queryId = queryId,
            eventBroker = eventBroker
         )
      }
   }

   private fun newSearchContext(clientQueryId: String? = Ids.id(prefix = "clientQueryId", size = 8)):QueryContext {
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
   fun only(fact: TypedInstance, scopedFacts: List<ScopedFact> = emptyList()): QueryContext {
      return only(listOf(fact), scopedFacts)
   }

   fun only(facts:List<TypedInstance>, scopedFacts: List<ScopedFact> = emptyList()): QueryContext {
      val copied = this.newSearchContext().copy(
         facts = CopyOnWriteFactBag(CopyOnWriteArrayList(facts), scopedFacts, schema),
         parent = this,
         vyneQueryStatistics = VyneQueryStatistics()
      )
      copied.excludedOperations.addAll(this.schema.excludedOperationsForEnrichment())
      copied.excludedServices.addAll(this.excludedServices)
      return copied
   }

   fun only(): QueryContext {
      val copied = this.newSearchContext()
      copied.excludedOperations.addAll(this.schema.excludedOperationsForEnrichment())
      copied.excludedServices.addAll(this.excludedServices)
      return copied
   }


   fun responseType(responseType: String?): QueryContext {
      this.responseType = responseType
      return this
   }

//   fun projectResultsTo(projectedTaxiType: lang.taxi.types.Type, scope:ProjectionFunctionScope?): QueryContext {
//      return projectResultsTo(ProjectionAnonymousTypeProvider.projectedTo(projectedTaxiType,schema), scope)
//   }

   override suspend fun findType(type: Type): Flow<TypedInstance> {
      return this.find(type.qualifiedName.parameterizedName)
         .results
   }

//   private fun projectResultsTo(targetType: Type, scope:ProjectionFunctionScope?): QueryContext {
//      projectResultsTo = targetType
//      projectionScope = scope
//      return this
//   }


   fun addEvaluatedEdge(evaluatedEdge: EvaluatedEdge) = this.evaluatedEdges.add(evaluatedEdge)


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

//      val (service, _) = OperationNames.serviceAndOperation(operation.vertex1.valueAsQualifiedName())
//      val invokedService = schema.service(service)
//      onServiceInvoked((invokedService))
//      if (result.source is OperationResult) {
//         eventBroker.reportRemoteOperationInvoked(result.source as OperationResult, this.queryId)
//      }
//      val operationCacheKey = ServiceInvocationCacheKey(operation.vertex1.value.toString(), callArgs)
//      getTopLevelContext().operationCache[operationCacheKey] = result
//      logger.debug { "Caching $operation [${operation.previousValue?.value} -> ${result.type.qualifiedName}]" }
//      return result
   }

   fun notifyOperationResult(
      operationName: String,
      result: TypedInstance,
      callArgs: Set<TypedInstance?>,
      addToOperationResultCache: Boolean = true
   ):TypedInstance {
      val (service, _) = OperationNames.serviceAndOperation(operationName)

      val invokedService = schema.service(service)
      onServiceInvoked((invokedService))
      if (result.source is OperationResult) {
         eventBroker.reportRemoteOperationInvoked(result.source as OperationResult, this.queryId)
      }
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

object NoOpQueryContextEventDispatcher : QueryContextEventDispatcher

interface RemoteCallOperationResultHandler : QueryContextEventHandler {
   fun recordResult(operation: OperationResult, queryId: String)
}
