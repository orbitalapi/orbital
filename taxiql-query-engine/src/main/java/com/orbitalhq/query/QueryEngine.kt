package com.orbitalhq.query

import com.google.common.base.Stopwatch
import com.orbitalhq.*
import com.orbitalhq.models.*
import com.orbitalhq.models.facts.ScopedFact
import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.query.graph.edges.EvaluatedEdge
import com.orbitalhq.query.graph.edges.ParameterFactory
import com.orbitalhq.query.graph.operationInvocation.OperationInvocationService
import com.orbitalhq.query.graph.operationInvocation.SearchRuntimeException
import com.orbitalhq.query.projection.ProjectionProvider
import com.orbitalhq.schemas.*
import com.orbitalhq.utils.StrategyPerformanceProfiler
import com.orbitalhq.utils.TimeBucketed
import com.orbitalhq.utils.timeBucketAsync
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import lang.taxi.mutations.Mutation
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}


open class SearchFailedException(
   message: String,
   val evaluatedPath: List<EvaluatedEdge>,
   val profilerOperation: ProfilerOperation,
   val failedAttempts: List<DataSource>
) : RuntimeException(message)

class UnresolvedTypeInQueryException(
   message: String,
   val typeName: QualifiedName,
   evaluatedPath: List<EvaluatedEdge>,
   profilerOperation: ProfilerOperation,
   failedAttempts: List<DataSource>
) : SearchFailedException(message, evaluatedPath, profilerOperation, failedAttempts)

interface QueryEngine {
   val operationInvocationService: OperationInvocationService
   val schema: Schema

   // This is a workaround, as state seems to be leaking all over the place.
   // We really want to either:
   // A) Entirely encapsulate state on the QueryContext
   //    (should be easier as the idea of ScopedFactMaps have evolved signficantly since
   //     the original StatefulQueryEngine was written)
   // or:
   // B) Understand why (A) isn't possible.
   fun newEngine(): QueryEngine
   fun newEngine(factSetMapFilter: (FactSetMap) -> FactSetMap): QueryEngine

   suspend fun find(
      type: Type,
      context: QueryContext,
      spec: TypedInstanceValidPredicate = AlwaysGoodSpec,
      applicableStrategiesPredicate: PermittedQueryStrategyPredicate = AllIsApplicableQueryStrategyPredicate,
      failureBehaviour: FailureBehaviour = FailureBehaviour.THROW
   ): QueryResult

   suspend fun find(
      queryString: QueryExpression,
      context: QueryContext,
      spec: TypedInstanceValidPredicate = AlwaysGoodSpec,
      applicableStrategiesPredicate: PermittedQueryStrategyPredicate = AllIsApplicableQueryStrategyPredicate,
      failureBehaviour: FailureBehaviour = FailureBehaviour.THROW
   ): QueryResult

   suspend fun find(
      target: QuerySpecTypeNode,
      context: QueryContext,
      spec: TypedInstanceValidPredicate = AlwaysGoodSpec,
      applicableStrategiesPredicate: PermittedQueryStrategyPredicate = AllIsApplicableQueryStrategyPredicate,
      failureBehaviour: FailureBehaviour = FailureBehaviour.THROW
   ): QueryResult

   suspend fun find(
      target: Set<QuerySpecTypeNode>,
      context: QueryContext,
      spec: TypedInstanceValidPredicate = AlwaysGoodSpec,
      applicableStrategiesPredicate: PermittedQueryStrategyPredicate = AllIsApplicableQueryStrategyPredicate,
      failureBehaviour: FailureBehaviour = FailureBehaviour.THROW
   ): QueryResult

   suspend fun find(
      target: QuerySpecTypeNode,
      context: QueryContext,
      excludedOperations: Set<SearchGraphExclusion<RemoteOperation>>,
      spec: TypedInstanceValidPredicate = AlwaysGoodSpec,
      applicableStrategiesPredicate: PermittedQueryStrategyPredicate = AllIsApplicableQueryStrategyPredicate,
      failureBehaviour: FailureBehaviour = FailureBehaviour.THROW
   ): QueryResult

   suspend fun findAll(queryString: QueryExpression, context: QueryContext): QueryResult

   fun queryContext(
      factSetIds: Set<FactSetId> = setOf(FactSets.DEFAULT),
      additionalFacts: Set<TypedInstance> = emptySet(),
      queryId: String,
      clientQueryId: String?,
      eventBroker: QueryContextEventBroker = QueryContextEventBroker(),
      scopedFacts: List<ScopedFact> = emptyList()
   ): QueryContext

   suspend fun build(type: Type, context: QueryContext): QueryResult =
      build(TypeNameQueryExpression(type.fullyQualifiedName), context)

   suspend fun build(query: QueryExpression, context: QueryContext): QueryResult

   // The inputValue allows for passing the result from a query.
   // Pass null if no upstream query exists
   suspend fun mutate(
      mutation: Mutation,
      spec: QuerySpecTypeNode,
      context: QueryContext,
      inputValue: TypedInstance?
   ): QueryResult

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
   private val initialState: FactSetMap,
   override val schema: Schema,
   private val strategies: List<QueryStrategy>,
   private val profiler: QueryProfiler = QueryProfiler(),
   private val projectionProvider: ProjectionProvider,
   override val operationInvocationService: OperationInvocationService,
   val formatSpecs: List<ModelFormatSpec>
) :
   QueryEngine,
   ModelContainer {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   private val factSets: FactSetMap = FactSetMap.create()


   init {
      factSets.putAll(initialState)
   }

   override fun newEngine(): QueryEngine {
      return StatefulQueryEngine(
         FactSetMap.create(),
         schema, strategies, profiler, projectionProvider, operationInvocationService, formatSpecs
      )
   }

   override fun newEngine(factSetMapFilter: (FactSetMap) -> FactSetMap): QueryEngine {
      val newFacts = factSetMapFilter(this.initialState)
      return StatefulQueryEngine(
         newFacts,
         schema, strategies, profiler, projectionProvider, operationInvocationService, formatSpecs
      )
   }

   override fun addModel(model: TypedInstance, factSetId: FactSetId): StatefulQueryEngine {
      this.factSets[factSetId].add(model)
      return this
   }


   override fun queryContext(
      factSetIds: Set<FactSetId>,
      additionalFacts: Set<TypedInstance>,
      queryId: String,
      clientQueryId: String?,
      eventBroker: QueryContextEventBroker,
      scopedFacts: List<ScopedFact>
   ): QueryContext {
      val facts = this.factSets.retainFactsFromFactSet(factSetIds).values().toSet()
      return QueryContext.from(
         schema,
         facts + additionalFacts,
         this,
         profiler,
         queryId = queryId,
         clientQueryId = clientQueryId,
         eventBroker = eventBroker,
         scopedFacts = scopedFacts
      )
   }

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
   // I'm starting this by treating find() and build() as separate operations, but
   // I'm not sure why...just a gut feel.
   // The idea use case here is for ETL style transformations, where a user may know
   // some, but not all, of the facts up front, and then use Vyne to polyfill.
   // Build starts by using facts known in it's current context to build the target type
   override suspend fun build(query: QueryExpression, context: QueryContext): QueryResult {

      val targetType = when (query) {
         is TypeNameQueryExpression -> {
            context.schema.type(query.typeName)
         }

         is TypeNameListQueryExpression -> {
            require(query.typeNames.size == 1) { "Currently, build only supports TypeNameQueryExpression, or a list of a single type" }
            context.schema.type(query.typeNames.first())
         }

         is TypeQueryExpression -> query.type
         else -> error("Currently, build only supports TypeNameQueryExpression")

      }
      // Note - this should be trivial to expand to TypeListQueryExpression too
//      val typeNameQueryExpression = when (query) {
//         is TypeNameQueryExpression -> query
//         is TypeNameListQueryExpression -> {
//            require(query.typeNames.size == 1) { "Currently, build only supports TypeNameQueryExpression, or a list of a single type" }
//            TypeNameQueryExpression(query.typeNames.first())
//         }
//         else -> error("Currently, build only supports TypeNameQueryExpression")
//      }
//      val targetType = context.schema.type(typeNameQueryExpression.typeName)
      return projectTo(targetType, context)
   }

   private suspend fun projectTo(targetType: Type, context: QueryContext): QueryResult {
      val isProjectingCollection =
         context.facts.isNotEmpty() && context.facts.stream().allMatch { it is TypedCollection }

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
            TypedCollection.arrayOf(
               targetType.collectionType!!,
               emptyList(),
               MixedSources.singleSourceOrMixedSources(context.facts)
            )
         }

         else -> {
            context.isProjecting = true
            objectBuilder(context, targetType).build()
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
            anonymousTypes = targetType.anonymousTypes,
            queryId = context.queryId,
            responseType = targetType.fullyQualifiedName,
            onCancelRequestHandler = { context.requestCancel() },
            schema = schema
         )
      } else {
         QueryResult(
            querySpecTypeNode,
            emptyFlow(),
            isFullyResolved = false,
            profilerOperation = context.profiler.root,
            queryId = context.queryId,
            clientQueryId = context.clientQueryId,
            anonymousTypes = targetType.anonymousTypes,
            responseType = targetType.fullyQualifiedName,
            onCancelRequestHandler = { context.requestCancel() },
            schema = schema
         )
      }
   }

   override suspend fun mutate(
      mutation: Mutation,
      spec: QuerySpecTypeNode,
      context: QueryContext,
      inputValue: TypedInstance?
   ): QueryResult {
      val service = schema.service(mutation.service.qualifiedName)
      val operation = service.operation(mutation.operation.name)

      val searchContext = if (inputValue != null) {
         context.only(inputValue)
      } else {
         context
      }
      val paramValues = ParameterFactory().discoverAll(operation, searchContext)

      // First pass.
      // TODO : Work out how to pass context (like facts from given clauses etc) into this.
      val resultFlow = invokeOperation(
         service,
         operation,
         // Current best guess of how to pass context
         setOfNotNull(inputValue),
         searchContext,
         paramValues
      )

      return QueryResult(
         spec,
         resultFlow,
         isFullyResolved = true,
         profilerOperation = context.profiler.root,
         anonymousTypes = spec.anonymousTypes(),
         queryId = context.queryId,
         responseType = spec.type.paramaterizedName,
         onCancelRequestHandler = { context.requestCancel() },
         schema = schema
      )
   }

   private fun objectBuilder(
      context: QueryContext,
      targetType: Type
   ) = ObjectBuilder(
      this,
      context,
      targetType,
      functionRegistry = this.schema.functionRegistry,
      formatSpecs = formatSpecs,
   )

   // TODO investigate why in tests got throught this method (there are two facts of TypedCollection), looks like this is only in tests
   private suspend fun projectCollection(targetType: Type, context: QueryContext): TypedInstance? {

      val targetCollectionType = if (targetType.isCollection) {
         targetType.resolveAliases().typeParameters[0]
      } else {
         targetType
      }

      logger.info("Mapping collections to collection of type ${targetCollectionType.qualifiedName} ")
      val transformed = context.facts
         .filterIsInstance<TypedCollection>()
         .flatMap { deeplyFlatten(it) }
         .map { typedInstance -> mapTo(targetCollectionType, typedInstance, context) }
         .mapNotNull { it }
      return if (transformed.isEmpty()) {
         null
      } else {
         TypedCollection.from(transformed, MixedSources.singleSourceOrMixedSources(transformed))
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
            logger.warn("Transformation from $typedInstance to instance of ${targetType.fullyQualifiedName} was reported as sucessful, but result was null")
         }
         result
      } else {
         logger.warn("Failed to transform from $typedInstance to instance of ${targetType.fullyQualifiedName}")
         null
      }
   }

   override fun parse(queryExpression: QueryExpression): Set<QuerySpecTypeNode> {
      return queryParser.parse(queryExpression)
   }

   override suspend fun find(
      queryString: QueryExpression,
      context: QueryContext,
      spec: TypedInstanceValidPredicate,
      applicableStrategiesPredicate: PermittedQueryStrategyPredicate,
      failureBehaviour: FailureBehaviour
   ): QueryResult {
      val target = queryParser.parse(queryString)
      return find(target, context, spec, applicableStrategiesPredicate, failureBehaviour)
   }

   override suspend fun find(
      type: Type,
      context: QueryContext,
      spec: TypedInstanceValidPredicate,
      applicableStrategiesPredicate: PermittedQueryStrategyPredicate,
      failureBehaviour: FailureBehaviour
   ): QueryResult {
      return find(TypeQueryExpression(type), context, spec, applicableStrategiesPredicate, failureBehaviour)
   }

   override suspend fun find(
      target: QuerySpecTypeNode,
      context: QueryContext,
      spec: TypedInstanceValidPredicate,
      applicableStrategiesPredicate: PermittedQueryStrategyPredicate,
      failureBehaviour: FailureBehaviour
   ): QueryResult {
      return find(setOf(target), context, spec, applicableStrategiesPredicate, failureBehaviour)
   }

   override suspend fun find(
      target: Set<QuerySpecTypeNode>,
      context: QueryContext,
      spec: TypedInstanceValidPredicate,
      applicableStrategiesPredicate: PermittedQueryStrategyPredicate,
      failureBehaviour: FailureBehaviour
   ): QueryResult {
      try {
         return doFind(target, context, spec, applicableStrategiesPredicate, failureBehaviour)
      } catch (e: QueryCancelledException) {
         logger.info("QueryCancelled. Coroutine active state: ${currentCoroutineContext().isActive}")
         throw e
      } catch (e: Exception) {
         logger.error("Search failed with exception:", e)
         throw SearchRuntimeException(e, context.profiler.root)
      }
   }

   override suspend fun find(
      target: QuerySpecTypeNode,
      context: QueryContext,
      excludedOperations: Set<SearchGraphExclusion<RemoteOperation>>,
      spec: TypedInstanceValidPredicate,
      applicableStrategiesPredicate: PermittedQueryStrategyPredicate,
      failureBehaviour: FailureBehaviour
   ): QueryResult {
      try {
         return doFind(
            target,
            context,
            spec,
            excludedOperations,
            applicableStrategiesPredicate,
            failureBehaviour = failureBehaviour
         )
      } catch (e: QueryCancelledException) {
         throw e
      } catch (e: Exception) {
         logger.error("Search failed with exception:", e)
         throw SearchRuntimeException(e, context.profiler.root)
      }
   }


   //TODO we still have places in the code that expect/consume a Set<QuerySpecTypeNode>
   private fun doFind(
      target: Set<QuerySpecTypeNode>,
      context: QueryContext,
      spec: TypedInstanceValidPredicate,
      applicableStrategiesPredicate: PermittedQueryStrategyPredicate,
      failureBehaviour: FailureBehaviour = FailureBehaviour.THROW
   ): QueryResult {

      val queryResult =
         doFind(
            target.first(),
            context,
            spec,
            applicableStrategiesPredicate = applicableStrategiesPredicate,
            failureBehaviour = failureBehaviour
         )

      return QueryResult(
         querySpec = queryResult.querySpec,
         results = queryResult.results,
         isFullyResolved = queryResult.isFullyResolved,
         profilerOperation = queryResult.profilerOperation,
         anonymousTypes = queryResult.anonymousTypes,
         queryId = context.queryId,
         clientQueryId = context.clientQueryId,
         statistics = queryResult.statistics,
         responseType = context.responseType,
         onCancelRequestHandler = { context.requestCancel() },
         schema = schema
      )

   }

   private fun doFind(
      target: QuerySpecTypeNode,
      context: QueryContext,
      spec: TypedInstanceValidPredicate,
      excludedOperations: Set<SearchGraphExclusion<RemoteOperation>> = emptySet(),
      applicableStrategiesPredicate: PermittedQueryStrategyPredicate,
      failureBehaviour: FailureBehaviour = FailureBehaviour.THROW,
   ): QueryResult {
      if (target.type.isPrimitive) {
         logger.warn { "A search was started for a primitive type (${target.type.qualifiedName.shortDisplayName} - this is almost certainly a bug" }
      }
      logger.debug { "Initiating find for ${target.description}" }
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
      val failedAttempts = mutableListOf<DataSource>()
      val resultsFlow: Flow<TypedInstance> = channelFlow {
         if (!isActive) {
            logger.warn { "Query ${context.queryId} has been cancelled - exiting" }
         }

         val applicableStrategies = strategies.filter { applicableStrategiesPredicate.isApplicable(it) }
         for (queryStrategy in applicableStrategies) {
            if (resultsReceivedFromStrategy || !isActive || context.cancelRequested) {
               break
            }
            val stopwatch = Stopwatch.createStarted()
            logger.debug { "Attempting strategy ${queryStrategy::class.simpleName} to resolve ${target.description}" }
            val strategyResult =
               invokeStrategy(context, queryStrategy, target, InvocationConstraints(spec, excludedOperations))
            failedAttempts.addAll(strategyResult.failedAttempts)
            if (strategyResult.hasMatchesNodes()) {
               strategyProvidedFlow = true
               strategyResult.matchedNodes
                  .onCompletion {
                     StrategyPerformanceProfiler.record(queryStrategy::class.simpleName!!, stopwatch.elapsed())
                  }
                  .takeWhile { !context.cancelRequested }
                  .collectIndexed { _, value ->
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
                     emitTypedInstances(valueAsCollection, !isActive, failedAttempts) { instance ->
                        if (instance is TypedNull) {
                           logger.debug { "Emitting TypedNull of type ${instance.type.qualifiedName.shortDisplayName} produced from strategy ${queryStrategy::class.simpleName} in search for ${target.description}" }
                        } else {
                           logger.debug { "Emitting instance of type ${instance.type.qualifiedName.shortDisplayName} produced from strategy ${queryStrategy::class.simpleName} in search for ${target.description}" }
                        }

                        send(instance)
                     }
                  }
            } else {
               logger.debug("Strategy ${queryStrategy::class.simpleName} failed to resolve ${target.description}")
               StrategyPerformanceProfiler.record(queryStrategy::class.simpleName!!, stopwatch.elapsed())
            }
         }

         if (!resultsReceivedFromStrategy) {
            val constraintsSuffix = if (target.dataConstraints.isNotEmpty()) {
               "with the ${target.dataConstraints.size} constraints provided"
            } else ""
            val message = "No data sources were found that can return ${target.description} $constraintsSuffix".trim()
            logger.debug { message }


            // I couldn't work out how to handle this.
            // When mapping, throwing an exception kills the other
            // mapping operations that are going on.
            // However, attempts to catch the exception haven't worked,
            // as we're not in the callers thread anymore, but inside an
            // async channel flow, somewhere.
            // So, leaving it to callers.
            when (failureBehaviour) {
               FailureBehaviour.SEND_TYPED_NULL -> {
                  send(
                     TypedNull.create(
                        target.type,
                        source = FailedSearch(message, failedAttempts)
                     )
                  )
               }

               FailureBehaviour.THROW -> {
                  if (strategyProvidedFlow) {
                     // We found a strategy which provided a flow of data, but the flow didn't yield any results.
                     // TODO : Should we just be closing here?  Perhaps we should emit some form of TypedNull,
                     // which would allow us to communicate the failed attempts?
                     close()
                  } else {
                     // We didn't find a strategy to provide any data.
                     throw UnresolvedTypeInQueryException(
                        message,
                        target.type.name,
                        emptyList(),
                        context,
                        failedAttempts
                     )
                  }
               }
            }
         }

      }.catch { exception ->
         if (exception !is CancellationException) {
            throw exception
         }
      }

      val projectedResults: Flow<Pair<TypedInstance, VyneQueryStatistics>> = when (target.projection) {
         null -> resultsFlow.map { it to context.vyneQueryStatistics }
         else -> {
            // Pick the facts that we want to make available during projection.
            // Currently, we pass the initial state (things from the `given {}` clause, and anything about the user).
            // We may wish to expand this.
            // The projection provider handles picking the correct entity to project,
            // so we don't need to consdier that here.
            val factsToPropagate = initialState.toFactBag(schema)
            projectionProvider.project(resultsFlow, target.projection, context, factsToPropagate)
         }
      }

      val querySpecTypeNode = if (target.projection != null) {
         QuerySpecTypeNode(target.projection.type, emptySet(), QueryMode.DISCOVER)
      } else {
         target
      }

      val mutatedResults: Flow<Pair<TypedInstance, VyneQueryStatistics>> = when (target.mutation) {
         null -> projectedResults
         else -> {
            // At this point, any queries have been executed, and we're ready
            // to invoke mutations.
            projectedResults.flatMapConcat { (queryResult, stats) ->
               mutate(target.mutation, target, context, queryResult)
                  .results.map {
                     it to stats
                  }
            }
         }
      }
      val statisticsFlow = MutableSharedFlow<VyneQueryStatistics>(replay = 0)
      return QueryResult(
         querySpecTypeNode,
         mutatedResults.onEach { statisticsFlow.emit(it.second) }.map { it.first },
         isFullyResolved = true,
         profilerOperation = context.profiler.root,
         queryId = context.queryId,
         clientQueryId = context.clientQueryId,
         anonymousTypes = target.anonymousTypes(),
         statistics = statisticsFlow,
         responseType = context.responseType,
         onCancelRequestHandler = { context.requestCancel() },
         schema = schema
      )

   }


   private suspend fun emitTypedInstances(
      valueAsCollection: List<TypedInstance>,
      cancelled: Boolean,
      failedAttempts: MutableList<DataSource>,
      send: suspend (instance: TypedInstance) -> Unit
   ) {
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
      val sw = Stopwatch.createStarted()
      val result =
         timeBucketAsync("Call ${queryStrategy::class.simpleName} for target ${target.type.name.shortDisplayName}") {
            queryStrategy.invoke(
               setOf(target),
               context,
               invocationConstraints
            )
         }
      val bucket = TimeBucketed.DEFAULT
      val successful = if (result.hasMatchesNodes()) {
         "Success"
      } else {
         "Failed"
      }
      val name = "Query Strategy ${queryStrategy::class.simpleName} - $successful"
      bucket.addActivity(name, sw.elapsed())
      val detailedName =
         "Invoke Query Strategy ${queryStrategy::class.simpleName} for target ${target.type.name.shortDisplayName} - $successful"
      bucket.addActivity(detailedName, sw.elapsed())
      return result

   }
}

class QueryCancelledException(message: String = "Query has been cancelled") : Exception(message)


class QueryFailedException(message: String) : Exception(message)
