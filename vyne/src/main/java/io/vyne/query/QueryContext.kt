package io.vyne.query

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.KeyDeserializer
import com.google.common.collect.HashMultimap
import io.vyne.models.EnumSynonyms
import io.vyne.models.CopyOnWriteFactBag
import io.vyne.models.FactBag
import io.vyne.models.FactDiscoveryStrategy
import io.vyne.models.InPlaceQueryEngine
import io.vyne.models.OperationResult
import io.vyne.models.RawObjectMapper
import io.vyne.models.TypeNamedInstance
import io.vyne.models.TypeNamedInstanceMapper
import io.vyne.models.TypedInstance
import io.vyne.models.TypedInstanceConverter
import io.vyne.query.ProjectionAnonymousTypeProvider.projectedTo
import io.vyne.query.QueryResponse.ResponseStatus
import io.vyne.query.QueryResponse.ResponseStatus.COMPLETED
import io.vyne.query.QueryResponse.ResponseStatus.INCOMPLETE
import io.vyne.query.graph.Element
import io.vyne.query.graph.EvaluatableEdge
import io.vyne.query.graph.EvaluatedEdge
import io.vyne.query.graph.ServiceAnnotations
import io.vyne.query.graph.ServiceParams
import io.vyne.schemas.Operation
import io.vyne.schemas.OperationNames
import io.vyne.schemas.OutputConstraint
import io.vyne.schemas.Parameter
import io.vyne.schemas.Policy
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Schema
import io.vyne.schemas.Service
import io.vyne.schemas.Type
import io.vyne.utils.StrategyPerformanceProfiler
import io.vyne.utils.orElse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import lang.taxi.policies.Instruction
import lang.taxi.types.ProjectedType
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

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
   override val queryId: String,
   @field:JsonIgnore // we send a lightweight version below
   val statistics: MutableSharedFlow<VyneQueryStatistics>? = null,
   override val responseType: String? = null
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

   val responseType: String?

}

interface FailedQueryResponse: QueryResponse {
   val message: String
   override val responseStatus: ResponseStatus
      get() = ResponseStatus.ERROR
   override val isFullyResolved: Boolean
      get() = false
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
      fun from(serializableVyneQueryStatistics:SerializableVyneQueryStatistics):VyneQueryStatistics {
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
)  {
  companion object {
     fun from(vyneQueryStatistics:VyneQueryStatistics):SerializableVyneQueryStatistics {
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

   ) : ProfilerOperation by profiler, FactBag by facts, QueryContextEventDispatcher, InPlaceQueryEngine {

   private val evaluatedEdges = mutableListOf<EvaluatedEdge>()
   private val policyInstructionCounts = mutableMapOf<Pair<QualifiedName, Instruction>, Int>()
   var isProjecting = false
   var projectResultsTo: Type? = null
      private set;

   var responseType: String? = null
      private set;

   private val cancelEmitter = Sinks.many().multicast().onBackpressureBuffer<QueryCancellationRequest>()
   val cancelFlux: Flux<QueryCancellationRequest> = cancelEmitter.asFlux()
   private var isCancelRequested: Boolean = false

   override fun requestCancel() {
      logger.info { "Cancelling query $queryId" }
      cancelEmitter.tryEmitNext(QueryCancellationRequest)
      isCancelRequested = true
   }

   val cancelRequested:Boolean
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

   suspend fun find(queryString: QueryExpression): QueryResult = queryEngine.find(queryString, this)
   suspend fun find(target: QuerySpecTypeNode): QueryResult = queryEngine.find(target, this)
   suspend fun find(target: Set<QuerySpecTypeNode>): QueryResult = queryEngine.find(target, this)
   suspend fun find(target: QuerySpecTypeNode, excludedOperations: Set<SearchGraphExclusion<Operation>>): QueryResult =
      queryEngine.find(target, this, excludedOperations)

   suspend fun build(typeName: QualifiedName): QueryResult = build(typeName.parameterizedName)
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
            CopyOnWriteFactBag(facts, schema),
            queryEngine,
            profiler,
            clientQueryId = clientQueryId,
            queryId = queryId,
            eventBroker = eventBroker
         )
      }
   }

   /**
    * Returns a QueryContext, with only the provided fact.
    * All other parameters (queryEngine, schema, etc) are retained
    */
   fun only(fact: TypedInstance): QueryContext {

      val mutableFacts = listOf(fact)
      val copied = this.copy(facts = CopyOnWriteFactBag(mutableFacts, schema), parent = this, vyneQueryStatistics = VyneQueryStatistics())
      copied.excludedOperations.addAll(this.schema.excludedOperationsForEnrichment())
      copied.excludedServices.addAll(this.excludedServices)
      return copied
   }

   fun only(): QueryContext {
      val copied = this.copy()
      copied.excludedOperations.addAll(this.schema.excludedOperationsForEnrichment())
      copied.excludedServices.addAll(this.excludedServices)
      return copied
   }


   fun responseType(responseType: String?): QueryContext {
      this.responseType = responseType
      return this
   }

   fun projectResultsTo(projectedType: ProjectedType): QueryContext {
      return projectResultsTo(projectedTo(projectedType, schema))
   }

   fun projectResultsTo(targetType: String): QueryContext {
      return projectResultsTo(ProjectedType.fromConcreteTypeOnly(schema.taxi.type(targetType)))
   }

   override suspend fun findType(type: Type): Flow<TypedInstance> {
      return this.find(type.qualifiedName.parameterizedName)
         .results
   }

   private fun projectResultsTo(targetType: Type): QueryContext {
      projectResultsTo = targetType
      return this
   }



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
      if (result.source is OperationResult) {
         eventBroker.reportRemoteOperationInvoked(result.source as OperationResult, this.queryId)
      }
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

   suspend fun invokeOperation(operationName: QualifiedName, preferredParams:Set<TypedInstance> = emptySet(), providedParamValues: List<Pair<Parameter, TypedInstance>> = emptyList()): Flow<TypedInstance> {
      val (service, operation) = this.schema.operation(operationName)
      return invokeOperation(service, operation)
   }
   suspend fun invokeOperation(service: Service, operation: Operation, preferredParams:Set<TypedInstance> = emptySet(), providedParamValues: List<Pair<Parameter, TypedInstance>> = emptyList()): Flow<TypedInstance> {
      return queryEngine.invokeOperation(
         service, operation,preferredParams, this, providedParamValues
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
   fun addHandlers(handlers:List<QueryContextEventHandler>):QueryContextEventBroker {
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
