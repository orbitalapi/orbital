package io.vyne.query

import com.google.common.base.Stopwatch
import io.vyne.FactSetId
import io.vyne.FactSetMap
import io.vyne.FactSets
import io.vyne.ModelContainer
import io.vyne.filterFactSets
import io.vyne.models.DataSource
import io.vyne.models.DataSourceUpdater
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.models.TypedObject
import io.vyne.query.graph.EvaluatedEdge
import io.vyne.query.graph.operationInvocation.OperationInvocationService
import io.vyne.query.graph.operationInvocation.SearchRuntimeException
import io.vyne.query.projection.ProjectionProvider
import io.vyne.schemas.Operation
import io.vyne.schemas.Parameter
import io.vyne.schemas.Schema
import io.vyne.schemas.Service
import io.vyne.schemas.Type
import io.vyne.utils.StrategyPerformanceProfiler
import io.vyne.utils.log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import mu.KotlinLogging
import reactor.core.Disposable
import java.util.function.Consumer
import java.util.stream.Collectors

private val logger = KotlinLogging.logger {}

open class SearchFailedException(
   message: String,
   val evaluatedPath: List<EvaluatedEdge>,
   val profilerOperation: ProfilerOperation,
   val failedAttempts: List<DataSource>
) : RuntimeException(message)

interface QueryEngine {
   val operationInvocationService: OperationInvocationService
   val schema: Schema
   suspend fun find(type: Type, context: QueryContext, spec: TypedInstanceValidPredicate = AlwaysGoodSpec): QueryResult
   suspend fun find(
      queryString: QueryExpression,
      context: QueryContext,
      spec: TypedInstanceValidPredicate = AlwaysGoodSpec
   ): QueryResult

   suspend fun find(
      target: QuerySpecTypeNode,
      context: QueryContext,
      spec: TypedInstanceValidPredicate = AlwaysGoodSpec
   ): QueryResult

   suspend fun find(
      target: Set<QuerySpecTypeNode>,
      context: QueryContext,
      spec: TypedInstanceValidPredicate = AlwaysGoodSpec
   ): QueryResult

   suspend fun find(
      target: QuerySpecTypeNode,
      context: QueryContext,
      excludedOperations: Set<SearchGraphExclusion<Operation>>,
      spec: TypedInstanceValidPredicate = AlwaysGoodSpec
   ): QueryResult

   suspend fun findAll(queryString: QueryExpression, context: QueryContext): QueryResult

   fun queryContext(
      factSetIds: Set<FactSetId> = setOf(FactSets.DEFAULT),
      additionalFacts: Set<TypedInstance> = emptySet(),
      queryId: String,
      clientQueryId: String?,
      eventBroker: QueryContextEventBroker = QueryContextEventBroker()
   ): QueryContext

   suspend fun build(type: Type, context: QueryContext): QueryResult =
      build(TypeNameQueryExpression(type.fullyQualifiedName), context)

   suspend fun build(query: QueryExpression, context: QueryContext): QueryResult

   fun parse(queryExpression: QueryExpression): Set<QuerySpecTypeNode>
   suspend fun invokeOperation(
      service: Service,
      operation: Operation,
      preferredParams: Set<TypedInstance>,
      context: QueryContext,
      providedParamValues: List<Pair<Parameter, TypedInstance>> = emptyList()
   ): Flow<TypedInstance> {
      return operationInvocationService.invokeOperation(
         service, operation, preferredParams, context, providedParamValues
      )
   }
}

/**
 * A query engine which allows for the provision of initial state
 */
class StatefulQueryEngine(
   initialState: FactSetMap,
   schema: Schema,
   strategies: List<QueryStrategy>,
   private val profiler: QueryProfiler = QueryProfiler(),
   projectionProvider: ProjectionProvider,
   operationInvocationService: OperationInvocationService
) :
   BaseQueryEngine(schema, strategies, projectionProvider, operationInvocationService), ModelContainer {
   private val factSets: FactSetMap = FactSetMap.create()

   init {
      factSets.putAll(initialState)
   }


//   override fun find(expression: String, factSet: Set<TypedInstance>): QueryResult {
//      val nodeSetsWithLocalState = factSets.copy()
//      nodeSetsWithLocalState.putAll(FactSets.DEFAULT, factSet)
//
//      return super.find(expression, nodeSetsWithLocalState.values().toSet())
//   }

   override fun addModel(model: TypedInstance, factSetId: FactSetId): StatefulQueryEngine {
      this.factSets[factSetId].add(model)
      return this
   }


   override fun queryContext(
      factSetIds: Set<FactSetId>,
      additionalFacts: Set<TypedInstance>,
      queryId: String,
      clientQueryId: String?,
      eventBroker: QueryContextEventBroker
   ): QueryContext {
      val facts = this.factSets.filterFactSets(factSetIds).values().toSet()
      return QueryContext.from(
         schema,
         facts + additionalFacts,
         this,
         profiler,
         queryId = queryId,
         clientQueryId = clientQueryId,
         eventBroker = eventBroker
      )
   }

}

// Note:  originally, there were two query engines (Default and Stateful), but only one was ever used (stateful).
// I've removed the default, and made it the BaseQueryEngine.  However, even this might be overkill, and we may
// fold this into a single class later.
// The separation between what's in the base and whats in the concrete impl. is not well thought out currently.
abstract class BaseQueryEngine(override val schema: Schema, private val strategies: List<QueryStrategy>, private val projectionProvider: ProjectionProvider, override val operationInvocationService: OperationInvocationService) : QueryEngine {

   private val queryParser = QueryParser(schema)

   override suspend fun findAll(queryString: QueryExpression, context: QueryContext): QueryResult {
      // First pass impl.
      // Thinking here is that if I can add a new Hipster strategy that discovers all the
      // endpoints, then I can compose a result of gather() from multiple finds()
      val findAllQuery = queryParser.parse(queryString).map { it.copy(mode = QueryMode.GATHER) }.toSet()
      // TODO return timed("BaseQueryEngine.findAll") {
      return find(findAllQuery, context)
      //}
   }

   // Experimental.
   // I'm starting this by treating find() and build() as seperate operations, but
   // I'm not sure why...just a gut feel.
   // The idea use case here is for ETL style transformations, where a user may know
   // some, but not all, of the facts up front, and then use Vyne to polyfill.
   // Build starts by using facts known in it's current context to build the target type
   override suspend fun build(query: QueryExpression, context: QueryContext): QueryResult {
      // Note - this should be trivial to expand to TypeListQueryExpression too
      val typeNameQueryExpression = when (query) {
         is TypeNameQueryExpression -> query
         is TypeNameListQueryExpression -> {
            require(query.typeNames.size == 1) { "Currently, build only supports TypeNameQueryExpression, or a list of a single type" }
            TypeNameQueryExpression(query.typeNames.first())
         }
         else -> error("Currently, build only supports TypeNameQueryExpression")
      }
      val targetType = context.schema.type(typeNameQueryExpression.typeName)
      return projectTo(targetType, context)
   }

   private suspend fun projectTo(targetType: Type, context: QueryContext): QueryResult {
      val isProjectingCollection = context.facts.stream().allMatch { it is TypedCollection }

      val querySpecTypeNode = QuerySpecTypeNode(targetType, emptySet(), QueryMode.DISCOVER)
      val result: TypedInstance? = when {
         //isCollectionToCollectionTransformation -> {
         //   mapCollectionToCollection(targetType, context)
         //}
         isProjectingCollection -> {
            projectCollection(targetType, context)
         }

         //isSingleToCollectionTransform -> {
         //mapSingleToCollection(targetType, context)
         //}
         targetType.isCollection && context.facts.all { it is TypedNull } -> {
            TypedCollection.arrayOf(targetType.collectionType!!, emptyList())
         }
         else -> {
            context.isProjecting = true
            ObjectBuilder(this, context, targetType, functionRegistry = this.schema.functionRegistry).build()
         }
      }
      val resultFlow = when (result) {
         null -> emptyFlow()
         is TypedCollection -> result.value.asFlow()
         else -> flowOf(result)
      }

      return if (result != null) {
         QueryResult(
            querySpecTypeNode,
            resultFlow,
            isFullyResolved = true,
            profilerOperation = context.profiler.root,
            anonymousTypes = context.schema.typeCache.anonymousTypes(),
            queryId = context.queryId,
            responseType = targetType.fullyQualifiedName
         )
      } else {
         QueryResult(
            querySpecTypeNode,
            emptyFlow(),
            isFullyResolved = false,
            profilerOperation = context.profiler.root,
            queryId = context.queryId,
            clientQueryId = context.clientQueryId,
            anonymousTypes = context.schema.typeCache.anonymousTypes(),
            responseType = targetType.fullyQualifiedName
         )
      }
   }

   private fun onlyTypedObject(context: QueryContext): TypedObject? {
      val typedObjects = context.facts.stream().filter { fact -> fact is TypedObject }.collect(Collectors.toList())
      return if (typedObjects.size == 1) {
         typedObjects[0] as TypedObject
      } else {
         null
      }
   }

   // TODO investigate why in tests got throught this method (there are two facts of TypedCollection), looks like this is only in tests
   private suspend fun projectCollection(targetType: Type, context: QueryContext): TypedInstance? {

      val targetCollectionType = if (targetType.isCollection) {
         targetType.resolveAliases().typeParameters[0]
      } else {
         targetType
      }

      log().info("Mapping collections to collection of type ${targetCollectionType.qualifiedName} ")
      val transformed = context.facts
         .filterIsInstance<TypedCollection>()
         .flatMap { deeplyFlatten(it) }
         .map { typedInstance -> mapTo(targetCollectionType, typedInstance, context) }
         .mapNotNull { it }
      return if (transformed.isEmpty()) {
         null
      } else {
         TypedCollection.from(transformed)
      }
   }

   /**
    * Recurses through TypedCollections, flattening any nested TypedCollections out,
    * so a single stream of TypedInstance is returned.
    * In practice, this issue of nested typed collections only appears to occur when using
    * the API to directly add parsed TypedObject into the query content
    */
   private fun deeplyFlatten(collection: TypedCollection): List<TypedInstance> {
      return collection.value.flatMap {
         when (it) {
            is TypedCollection -> deeplyFlatten(it)
            else -> listOf(it)
         }
      }
   }


   private suspend fun mapTo(targetType: Type, typedInstance: TypedInstance, context: QueryContext): TypedInstance? {

      //paramitziedtype of
      val transformationResult = context.only(typedInstance).build(targetType.fullyQualifiedName)

      return if (transformationResult.isFullyResolved) {
         val results = transformationResult.results.toList()
         require(results.size == 1) { "Expected only a single transformation result" }
         val result = results.first()
         if (result == null) {
            log().warn("Transformation from $typedInstance to instance of ${targetType.fullyQualifiedName} was reported as sucessful, but result was null")
         }
         result
      } else {
         log().warn("Failed to transform from $typedInstance to instance of ${targetType.fullyQualifiedName}")
         null
      }
   }

   override fun parse(queryExpression: QueryExpression): Set<QuerySpecTypeNode> {
      return queryParser.parse(queryExpression)
   }

   override suspend fun find(
      queryString: QueryExpression,
      context: QueryContext,
      spec: TypedInstanceValidPredicate
   ): QueryResult {
      val target = queryParser.parse(queryString)
      return find(target, context, spec)
   }

   override suspend fun find(type: Type, context: QueryContext, spec: TypedInstanceValidPredicate): QueryResult {
      return find(TypeNameQueryExpression(type.name.parameterizedName), context, spec)
   }

   override suspend fun find(
      target: QuerySpecTypeNode,
      context: QueryContext,
      spec: TypedInstanceValidPredicate
   ): QueryResult {
      return find(setOf(target), context, spec)
   }

   override suspend fun find(
      target: Set<QuerySpecTypeNode>,
      context: QueryContext,
      spec: TypedInstanceValidPredicate
   ): QueryResult {
      try {
         return doFind(target, context, spec)
      } catch (e: QueryCancelledException) {
         throw e
      } catch (e: Exception) {
         log().error("Search failed with exception:", e)
         throw SearchRuntimeException(e, context.profiler.root)
      }
   }

   override suspend fun find(
      target: QuerySpecTypeNode,
      context: QueryContext,
      excludedOperations: Set<SearchGraphExclusion<Operation>>,
      spec: TypedInstanceValidPredicate
   ): QueryResult {
      try {
         return doFind(target, context, spec, excludedOperations)
      } catch (e: QueryCancelledException) {
         throw e
      } catch (e: Exception) {
         log().error("Search failed with exception:", e)
         throw SearchRuntimeException(e, context.profiler.root)
      }
   }


   //TODO we still have places in the code that expect/consume a Set<QuerySpecTypeNode>
   private fun doFind(
      target: Set<QuerySpecTypeNode>,
      context: QueryContext,
      spec: TypedInstanceValidPredicate
   ): QueryResult {


      // TODO : BIG opportunity to optimize this by evaluating multiple querySpecNodes at once.
      // Which would allow us to be smarter about results we collect from rest calls.
      // Optimize later.
      //target.get(0).map { doFind(it, context, spec) }

      val queryResult = doFind(target.first(), context, spec)

      return QueryResult(
         querySpec = queryResult.querySpec,
         results = queryResult.results,
         isFullyResolved = queryResult.isFullyResolved,
         profilerOperation = queryResult.profilerOperation,
         anonymousTypes = queryResult.anonymousTypes,
         queryId = context.queryId,
         clientQueryId = context.clientQueryId,
         statistics = queryResult.statistics,
         responseType = context.responseType
      )

   }

   private fun doFind(
      target: QuerySpecTypeNode,
      context: QueryContext,
      spec: TypedInstanceValidPredicate,
      excludedOperations: Set<SearchGraphExclusion<Operation>> = emptySet()
   ): QueryResult {
      if (context.cancelRequested) {
         throw QueryCancelledException()
      }

      // Note: We used to take a set<QuerySpecTypeNode>, but currently only take a single.
      // We'll likely re-optimize to take multiple, but for now wrap in a set.
      // This can (and should) change in the future
      //val querySet = setOf(target)

      // This is cheating, probably.
      // We only resolve top-level nodes, rather than traverse deeply.
      //fun unresolvedNodes(): List<QuerySpecTypeNode> {
      //   return querySet.filterNot { matchedNodes.containsKey(it) }
      //}
      var resultsReceivedFromStrategy = false
      // Indicates if any strategy has provided a flow of results.
      // We use the presence/ absence of a flow to signal the difference between
      // "Unable to perform this search" (flow is null), and "Performed the search (but might not produce results)" (flow present)
      var strategyProvidedFlow = false
      var cancellationSubscription: Disposable? = null;
      val failedAttempts = mutableListOf<DataSource>()
      val resultsFlow: Flow<TypedInstance> = channelFlow {
         var cancelled = false
         cancellationSubscription = context.cancelFlux.subscribe {
            logger.info { "QueryEngine for queryId ${context.queryId} is cancelling" }
            cancel("Query cancelled at user request", QueryCancelledException())
            cancelled = true
         }


         for (queryStrategy in strategies) {
            if (resultsReceivedFromStrategy || cancelled) {
               break
            }
            val stopwatch = Stopwatch.createStarted()
            val strategyResult =
               invokeStrategy(context, queryStrategy, target, InvocationConstraints(spec, excludedOperations))
            failedAttempts.addAll(strategyResult.failedAttempts)
            if (strategyResult.hasMatchesNodes()) {
               strategyProvidedFlow = true
               strategyResult.matchedNodes
                  .onCompletion {
                     StrategyPerformanceProfiler.record(queryStrategy::class.simpleName!!, stopwatch.elapsed())
                  }
                  .collectIndexed { index, value ->
                     resultsReceivedFromStrategy = true
                     // We may have received a TypedCollection upstream (ie., from a service
                     // that returns Foo[]).  Given we treat everything as a flow of results,
                     // we don't want consumers to receive a result that is a collection (as it makes the
                     // result contract awkward), so unwrap any collection
                     val valueAsCollection = if (value is TypedCollection) {
                        value.value
                     } else {
                        listOf(value)
                     }
                     emitTypedInstances(valueAsCollection, cancelled, failedAttempts) { instance -> send(instance)}
                  }
            } else {
               log().debug("Strategy ${queryStrategy::class.simpleName} failed to resolve ${target.description}")
               StrategyPerformanceProfiler.record(queryStrategy::class.simpleName!!, stopwatch.elapsed())
            }
         }

         if (!resultsReceivedFromStrategy) {
            val constraintsSuffix = if (target.dataConstraints.isNotEmpty()) {
               "with the ${target.dataConstraints.size} constraints provided"
            } else ""
            logger.debug { "No strategy found for discovering type ${target.description} $constraintsSuffix".trim() }
            if (strategyProvidedFlow) {
               // We found a strategy which provided a flow of data, but the flow didn't yield any results.
               // TODO : Should we just be closing here?  Perhaps we should emit some form of TypedNull,
               // which would allow us to communicate the failed attempts?

               close()
            } else {
               // We didn't find a strategy to provide any data.
               throw SearchFailedException(
                  "No strategy found for discovering type ${target.description} $constraintsSuffix".trim(),
                  emptyList(),
                  context,
                  failedAttempts
               )
            }
         }

      }.onCompletion {
         cancellationSubscription?.dispose()
      }
         .catch { exception ->
            if (exception !is CancellationException) {
               throw exception
            }
         }

      val results:Flow<Pair<TypedInstance, VyneQueryStatistics>> = when (context.projectResultsTo) {
         null -> resultsFlow.map { it to context.vyneQueryStatistics}
         else -> {
            projectionProvider.project(resultsFlow, context)
         }
      }

      val querySpecTypeNode = if (context.projectResultsTo != null) {
         QuerySpecTypeNode(context.projectResultsTo!!, emptySet(), QueryMode.DISCOVER)
      } else {
         target
      }

      val statisticsFlow = MutableSharedFlow<VyneQueryStatistics>(replay = 0)
      return QueryResult(
         querySpecTypeNode,
         results.onEach { statisticsFlow.emit(it.second) }.map { it.first },
         isFullyResolved = true,
         profilerOperation = context.profiler.root,
         queryId = context.queryId,
         clientQueryId = context.clientQueryId,
         anonymousTypes = context.schema.typeCache.anonymousTypes(),
         statistics = statisticsFlow,
         responseType = context.responseType
      )

   }


   private suspend fun emitTypedInstances(
      valueAsCollection: List<TypedInstance>,
      cancelled: Boolean,
      failedAttempts: MutableList<DataSource>,
      send: suspend (instance: TypedInstance) -> Unit) {
      valueAsCollection.forEach { collectionMember ->
         if (!cancelled) {
            val valueToSend = if (failedAttempts.isNotEmpty()) {
               DataSourceUpdater.update(collectionMember, collectionMember.source.appendFailedAttempts(failedAttempts))
            } else {
               collectionMember
            }
            send(valueToSend)
         } else {
            currentCoroutineContext().cancel()
         }
      }
   }

   private suspend fun invokeStrategy(
      context: QueryContext,
      queryStrategy: QueryStrategy,
      target: QuerySpecTypeNode,
      invocationConstraints: InvocationConstraints
   ): QueryStrategyResult {
      return if (context.debugProfiling) {
         //context.startChild(this, "Query with ${queryStrategy.javaClass.simpleName}", OperationType.GRAPH_TRAVERSAL) { op ->
         //op.addContext("Search target", querySet.map { it.type.fullyQualifiedName })
         queryStrategy.invoke(setOf(target), context, invocationConstraints)
         //}
      } else {
         return queryStrategy.invoke(setOf(target), context, invocationConstraints)
      }
   }
}

class QueryCancelledException(message: String = "Query has been cancelled") : Exception(message)



