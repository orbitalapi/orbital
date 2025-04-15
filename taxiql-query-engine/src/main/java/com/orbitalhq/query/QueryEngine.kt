package com.orbitalhq.query

import com.google.common.base.Stopwatch
import com.orbitalhq.FactSetId
import com.orbitalhq.FactSetMap
import com.orbitalhq.FactSets
import com.orbitalhq.ModelContainer
import com.orbitalhq.errors.ErrorType
import com.orbitalhq.metrics.NoOpMetricsReporter
import com.orbitalhq.metrics.QueryMetricsReporter
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.DataSourceUpdater
import com.orbitalhq.models.FailedEvaluation
import com.orbitalhq.models.FailedSearch
import com.orbitalhq.models.MixedSources
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.QueryFailureBehaviour
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.facts.ScopedFact
import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.mutations.MutationProps
import com.orbitalhq.query.collections.CollectionBuilder
import com.orbitalhq.query.graph.edges.EvaluatedEdge
import com.orbitalhq.query.graph.edges.ParameterFactory
import com.orbitalhq.query.graph.operationInvocation.OperationInvocationService
import com.orbitalhq.query.graph.operationInvocation.SearchRuntimeException
import com.orbitalhq.query.projection.ProjectionProvider
import com.orbitalhq.retainFactsFromFactSet
import com.orbitalhq.schemas.Operation
import com.orbitalhq.schemas.OperationInvocationException
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.Type
import com.orbitalhq.toFactBag
import com.orbitalhq.utils.NoStackException
import com.orbitalhq.utils.StrategyPerformanceProfiler
import com.orbitalhq.utils.TimeBucketed
import com.orbitalhq.utils.timeBucketAsync
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.isActive
import lang.taxi.mutations.Mutation
import mu.KotlinLogging
import org.slf4j.MDC
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger {}


open class SearchFailedException(
   message: String,
   val evaluatedPath: List<EvaluatedEdge>,
   val profilerOperation: ProfilerOperation,
   val failedAttempts: List<DataSource>
) : NoStackException(message)

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
      failureBehaviour: QueryFailureBehaviour = QueryFailureBehaviour.THROW,
      metricsTags: MetricTags = MetricTags.NONE
   ): QueryResult

   suspend fun find(
      queryString: QueryExpression,
      context: QueryContext,
      spec: TypedInstanceValidPredicate = AlwaysGoodSpec,
      applicableStrategiesPredicate: PermittedQueryStrategyPredicate = AllIsApplicableQueryStrategyPredicate,
      failureBehaviour: QueryFailureBehaviour = QueryFailureBehaviour.THROW,
      metricsTags: MetricTags = MetricTags.NONE
   ): QueryResult

   suspend fun find(
      target: QuerySpecTypeNode,
      context: QueryContext,
      spec: TypedInstanceValidPredicate = AlwaysGoodSpec,
      applicableStrategiesPredicate: PermittedQueryStrategyPredicate = AllIsApplicableQueryStrategyPredicate,
      failureBehaviour: QueryFailureBehaviour = QueryFailureBehaviour.THROW,
      metricsTags: MetricTags = MetricTags.NONE
   ): QueryResult

   suspend fun find(
      target: Set<QuerySpecTypeNode>,
      context: QueryContext,
      spec: TypedInstanceValidPredicate = AlwaysGoodSpec,
      applicableStrategiesPredicate: PermittedQueryStrategyPredicate = AllIsApplicableQueryStrategyPredicate,
      failureBehaviour: QueryFailureBehaviour = QueryFailureBehaviour.THROW,
      metricsTags: MetricTags = MetricTags.NONE
   ): QueryResult

   suspend fun find(
      target: QuerySpecTypeNode,
      context: QueryContext,
      excludedOperations: Set<SearchGraphExclusion<RemoteOperation>>,
      spec: TypedInstanceValidPredicate = AlwaysGoodSpec,
      applicableStrategiesPredicate: PermittedQueryStrategyPredicate = AllIsApplicableQueryStrategyPredicate,
      failureBehaviour: QueryFailureBehaviour = QueryFailureBehaviour.THROW,
      metricsTags: MetricTags = MetricTags.NONE
   ): QueryResult

   suspend fun findAll(
      queryString: QueryExpression, context: QueryContext,
      metricsTags: MetricTags = MetricTags.NONE
   ): QueryResult

   fun queryContext(
      factSetIds: Set<FactSetId> = setOf(FactSets.DEFAULT),
      additionalFacts: Set<TypedInstance> = emptySet(),
      queryId: String,
      clientQueryId: String?,
      eventBroker: QueryContextEventBroker = QueryContextEventBroker(),
      scopedFacts: List<ScopedFact> = emptyList(),
      queryOptions: QueryOptions = QueryOptions.default()
   ): QueryContext

   suspend fun build(type: Type, context: QueryContext): QueryResult =
      build(TypeNameQueryExpression(type.fullyQualifiedName), context)

   suspend fun build(
      query: QueryExpression,
      context: QueryContext,
      metricsTags: MetricTags = MetricTags.NONE
   ): QueryResult

   // The inputValue allows for passing the result from a query.
   // Pass null if no upstream query exists
   suspend fun mutate(
      mutation: Mutation,
      spec: QuerySpecTypeNode,
      context: QueryContext,
      inputValue: TypedInstance?,
      metricsTags: MetricTags = MetricTags.NONE
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
   val formatSpecs: List<ModelFormatSpec>,
   private val metricsReporter: QueryMetricsReporter = NoOpMetricsReporter
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
         schema,
         strategies,
         profiler,
         projectionProvider,
         operationInvocationService,
         formatSpecs,
         metricsReporter
      )
   }

   override fun newEngine(factSetMapFilter: (FactSetMap) -> FactSetMap): QueryEngine {
      val newFacts = factSetMapFilter(this.initialState)
      return StatefulQueryEngine(
         newFacts,
         schema,
         strategies,
         profiler,
         projectionProvider,
         operationInvocationService,
         formatSpecs,
         metricsReporter
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
      scopedFacts: List<ScopedFact>,
      queryOptions: QueryOptions
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
         scopedFacts = scopedFacts,
         queryOptions = queryOptions,
         metricsReporter = metricsReporter
      )
   }

   private val queryParser = QueryParser(schema)

   override suspend fun findAll(
      queryString: QueryExpression, context: QueryContext,
      metricsTags: MetricTags
   ): QueryResult {
      val findAllQuery = queryParser.parse(queryString).map { it.copy(mode = QueryMode.GATHER) }.toSet()
      return find(findAllQuery, context, metricsTags = metricsTags)
   }

   override suspend fun build(query: QueryExpression, context: QueryContext, metricsTags: MetricTags): QueryResult {
      val targetType = when (query) {
         is TypeNameQueryExpression -> {
            context.schema.type(query.typeName)
         }

         is TypeQueryExpression -> query.type
         else -> error("Currently, build only supports TypeNameQueryExpression")

      }

      return projectTo(targetType, context, metricsTags)
   }

   private suspend fun projectTo(
      targetType: Type,
      context: QueryContext,
      metricTags: MetricTags = MetricTags.NONE
   ): QueryResult {
      // We're capturing metrics inside projections
      // as for streaming queries, this is where the bulk of the work happens.
      // This doesn't lead to nested metrics, as only streaming queries are propogating their metrics tags
      val isProjectingCollection =
         context.facts.isNotEmpty() && context.facts.stream().allMatch { it is TypedCollection }

      val querySpecTypeNode = QuerySpecTypeNode(targetType, expression = null, emptySet(), QueryMode.DISCOVER)
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
            responseType = querySpecTypeNode.type,
            onCancelRequestHandler = { context.requestCancel() },
            schema = schema,
            errors =  context.eventBroker.queryErrorPublisher.errors
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
            responseType = querySpecTypeNode.type,
            onCancelRequestHandler = { context.requestCancel() },
            schema = schema,
            errors =  context.eventBroker.queryErrorPublisher.errors
         )
      }
   }

   override suspend fun mutate(
      mutation: Mutation,
      spec: QuerySpecTypeNode,
      context: QueryContext,
      inputValue: TypedInstance?,
      metricsTags: MetricTags
   ): QueryResult {
      val startTime = Instant.now()
      // First pass.
      // TODO : Work out how to pass context (like facts from given clauses etc) into this.
      val (resultFlow, searchContext) = doMutate(mutation, context, inputValue)
      val resultFlowWithProcessingTime = resultFlow.map { it.withProcessingMetadata(asOf = startTime) }
      val resultsWithProjections = performMutationProjection(
         context,
         resultFlowWithProcessingTime,
         mutation)

      val metricsCapturedResultStream = context.metricsReporter.observeEventStream(
         resultsWithProjections, startTime, metricsTags, false
      )

      return QueryResult(
         spec,
         metricsCapturedResultStream,
         isFullyResolved = true,
         profilerOperation = context.profiler.root,
         anonymousTypes = spec.anonymousTypes(),
         queryId = context.queryId,
         responseType = spec.type,
         onCancelRequestHandler = { context.requestCancel() },
         schema = schema,
         responseHeaders = searchContext.populateResponseHeaders(),
         errors = context.eventBroker.queryErrorPublisher.errors
      )
   }

   private suspend fun doMutate(
      mutation: Mutation,
      context: QueryContext,
      inputValue: TypedInstance?,
   ): Pair<Flow<TypedInstance>, QueryContext> {
      val service = schema.service(mutation.service.qualifiedName)
      val operation = service.operation(mutation.operation.name)

      val searchContext = if (inputValue != null) {
         // include scoped facts. These are things that have been passed in the global given {} clause, etc
         context.only(inputValue, scopedFacts = context.scopedFacts)
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
      ).catch {
         context.eventBroker.queryErrorPublisher.onError(context.queryId,
            StreamErrorMessage.fromThrowable(it, operation.returnType.paramaterizedName))
         val dataSource = when (it) {
            is OperationInvocationException -> OperationResult.from(it.parameters, it.remoteCall)
               .asOperationReferenceDataSource()

            else -> FailedEvaluation("An error occurred when invoking operation ${operation.qualifiedName.longDisplayName}: ${it.message} ")
         }

         logger.warn { "Operation ${operation.qualifiedName} failed with exception ${it.message}. " }
         emit(TypedNull.create(operation.returnType, dataSource))
      }
      return resultFlow to searchContext
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


      // MP 3-Oct-24:
      // Adding the scoped facts here for traversal.
      // I think these were exclulded because it can muddy the context, but they're
      // explicitly defined as in scope
      // eg:
      //  find { Movie } as (cast:Actor[]) -> {
      //               starring : Actor[] by cast.filter( (Name) -> Name == 'Mark' )
      //            }
      // the variable 'cast' is in scope here.
      val transformationResult = context.only(typedInstance, context.scopedFacts).build(targetType)

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
      failureBehaviour: QueryFailureBehaviour,
      metricsTags: MetricTags
   ): QueryResult {
      val target = queryParser.parse(queryString)
      return find(target, context, spec, applicableStrategiesPredicate, failureBehaviour, metricsTags)
   }

   override suspend fun find(
      type: Type,
      context: QueryContext,
      spec: TypedInstanceValidPredicate,
      applicableStrategiesPredicate: PermittedQueryStrategyPredicate,
      failureBehaviour: QueryFailureBehaviour,
      metricsTags: MetricTags
   ): QueryResult {
      return find(
         TypeQueryExpression(type),
         context,
         spec,
         applicableStrategiesPredicate,
         failureBehaviour,
         metricsTags
      )
   }

   override suspend fun find(
      target: QuerySpecTypeNode,
      context: QueryContext,
      spec: TypedInstanceValidPredicate,
      applicableStrategiesPredicate: PermittedQueryStrategyPredicate,
      failureBehaviour: QueryFailureBehaviour,
      metricsTags: MetricTags
   ): QueryResult {
      return find(setOf(target), context, spec, applicableStrategiesPredicate, failureBehaviour, metricsTags)
   }

   override suspend fun find(
      target: Set<QuerySpecTypeNode>,
      context: QueryContext,
      spec: TypedInstanceValidPredicate,
      applicableStrategiesPredicate: PermittedQueryStrategyPredicate,
      failureBehaviour: QueryFailureBehaviour,
      metricsTags: MetricTags
   ): QueryResult {
      try {
         return doFind(target, context, spec, applicableStrategiesPredicate, failureBehaviour, metricsTags)
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
      failureBehaviour: QueryFailureBehaviour,
      metricsTags: MetricTags
   ): QueryResult {
      try {
         return doFind(
            target,
            context,
            spec,
            excludedOperations,
            applicableStrategiesPredicate,
            failureBehaviour = failureBehaviour,
            metricsTags = metricsTags
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
      failureBehaviour: QueryFailureBehaviour = QueryFailureBehaviour.THROW,
      metricsTags: MetricTags
   ): QueryResult {

      val queryResult = when {
         // Is this a multi-stream join?
         target.size > 1 && target.all { it.type.isStream } -> {
            TODO("Streaming")
         }

         target.size == 1 -> doFind(
            target.first(),
            context,
            spec,
            applicableStrategiesPredicate = applicableStrategiesPredicate,
            failureBehaviour = failureBehaviour,
            metricsTags = metricsTags
         )

         else -> error("Querying with multiple targets is not supported")

      }

      return QueryResult(
         querySpec = queryResult.querySpec,
         results = queryResult.results,
         isFullyResolved = queryResult.isFullyResolved,
         profilerOperation = queryResult.profilerOperation,
         anonymousTypes = queryResult.anonymousTypes,
         queryId = context.queryId,
         clientQueryId = context.clientQueryId,
         responseType = queryResult.querySpec.type,
         onCancelRequestHandler = { context.requestCancel() },
         schema = schema,
         responseHeaders = context.populateResponseHeaders(),
         errors = context.eventBroker.queryErrorPublisher.errors
      )

   }

   private fun doFind(
      target: QuerySpecTypeNode,
      context: QueryContext,
      spec: TypedInstanceValidPredicate,
      excludedOperations: Set<SearchGraphExclusion<RemoteOperation>> = emptySet(),
      applicableStrategiesPredicate: PermittedQueryStrategyPredicate,
      failureBehaviour: QueryFailureBehaviour = QueryFailureBehaviour.THROW,
      metricsTags: MetricTags
   ): QueryResult {
      val queryStartTime = Instant.now()
      val isStreamingQuery = target.type.isStream
      val targetIsCollection = target.type.isCollection
      if (target.type.isPrimitive && target.expression != null) {
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
      val contextMap = MDC.getCopyOfContextMap()
      val resultsFlow: Flow<TypedInstance> = channelFlow {
         // re-populate the QueryId for the channel flow collection.
         MDC.setContextMap(contextMap)
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
                  .collectIndexed { _, value ->
                     resultsReceivedFromStrategy = true

                     if (value.typeName == ErrorType.type.paramaterizedName) {
                        throw IllegalStateException(value.value!! as String)
                        //  close()
                     }

                     // We may have received a TypedCollection upstream (ie., from a service
                     // that returns Foo[]).  Given we treat everything as a flow of results,
                     // we don't want consumers to receive a result that is a collection (as it makes the
                     // result contract awkward), so unwrap any collection
                     val valueAsCollection = if (value is TypedCollection) {
                        value.value
                     } else {
                        listOf(value)
                     }

                     emitTypedInstances(
                        valueAsCollection,
                        (!isActive || context.cancelRequested),
                        failedAttempts
                     ) { instance ->
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

            suspend fun sendNull() {
               send(
                  TypedNull.create(
                     target.type,
                     source = FailedSearch(message, failedAttempts)
                  )
               )
            }

            // I couldn't work out how to handle this.
            // When mapping, throwing an exception kills the other
            // mapping operations that are going on.
            // However, attempts to catch the exception haven't worked,
            // as we're not in the callers thread anymore, but inside an
            // async channel flow, somewhere.
            // So, leaving it to callers.
            when (failureBehaviour) {
               QueryFailureBehaviour.SEND_TYPED_NULL -> sendNull()
               QueryFailureBehaviour.SEND_TYPED_NULL_OR_EMPTY_ARRAY -> {
                  if (target.type.isCollection) {
                     send(TypedCollection.empty(target.type))
                  } else {
                     sendNull()
                  }
               }

               QueryFailureBehaviour.THROW -> {
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
         MDC.setContextMap(contextMap)
         when (exception) {
            !is CancellationException -> {
               metricsReporter.failed(Duration.between(queryStartTime, Instant.now()), metricsTags)
               if (exception !is UnresolvedTypeInQueryException) {
                  context.eventBroker.queryErrorPublisher.onError(
                     context.queryId,
                     StreamErrorMessage.fromThrowable(exception, target.type.paramaterizedName)
                  )
               }
               throw exception
            }

            else -> {
               if (context.cancelRequested) {
                  throw exception
               }
            }
         }
      }

      val projectedResults: Flow<TypedInstanceWithMetadata> = when (target.projection) {
         null -> resultsFlow.map {
            // When working with streaming, we consider the "start" of the processing
            // window as when the message arrived, not when the query started.
            if (isStreamingQuery) {
               it.withProcessingMetadata()
            } else {
               it.withProcessingMetadata(asOf = queryStartTime)
            }
         }

         else -> {
            // Pick the facts that we want to make available during projection.
            // Currently, we pass the initial state (things from the `given {}` clause, and anything about the user).
            // We may wish to expand this.
            // The projection provider handles picking the correct entity to project,
            // so we don't need to consider that here.
            val factsToPropagate = initialState.toFactBag(schema)
            projectionProvider.project(resultsFlow, target.type, target.projection, context, factsToPropagate)
               .map { projectedInstanceWithMetadata ->
                  if (!isStreamingQuery) {
                     projectedInstanceWithMetadata.copy(processingStart = queryStartTime)
                  } else {
                     projectedInstanceWithMetadata
                  }

               }
         }
      }

      val querySpecTypeNode = if (target.projection != null) {
         QuerySpecTypeNode(target.projection.type, expression = null, emptySet(), QueryMode.DISCOVER)
      } else {
         target
      }


      val mutatedResults: Flow<TypedInstanceWithMetadata> = when (target.mutation) {
         null -> projectedResults
         else ->
            performMutationProjection(
               context,
               performMutation(target, projectedResults, context),
               target.mutation)
      }


      // When it's a streaming query, we log the duration of each individual message, rather than the query as a whole,
      // which could never end.
      val logDurationsOfIndividualMessages = isStreamingQuery
      val metricsCapturedResultStream = context.metricsReporter.observeEventStream(
         mutatedResults, queryStartTime, metricsTags, logDurationsOfIndividualMessages
      )

      val anonymousTypes = if (schema is QuerySchema) {
         // Inline types defined in a schema include types defined in the
         // query type - such as an inline Union type when joining streams.
         (schema.inlineTypes + target.anonymousTypes()).toSet()
      } else {
         target.anonymousTypes()
      }
      return QueryResult(
         querySpecTypeNode,
         results = metricsCapturedResultStream,
         isFullyResolved = true,
         profilerOperation = context.profiler.root,
         queryId = context.queryId,
         clientQueryId = context.clientQueryId,
         anonymousTypes = anonymousTypes,
         responseType = querySpecTypeNode.type,
         onCancelRequestHandler = { context.requestCancel() },
         schema = schema,
         errors = context.eventBroker.queryErrorPublisher.errors
      )

   }

   private fun performMutation(
      target: QuerySpecTypeNode,
      projectedResults: Flow<TypedInstanceWithMetadata>,
      context: QueryContext
   ): Flow<TypedInstanceWithMetadata> {
      val mutationProps = MutationProps.from(target.mutation, target.type, schema)
      if (mutationProps != null && !mutationProps.isValid) {
         throw IllegalArgumentException(mutationProps.validationError)
      }
      return if (mutationProps?.mutationOperationHasCollectionParameters == true) {
         val typedCollection = CollectionBuilder.toCollectionType(projectedResults)
         projectionProvider.process(flowOf(typedCollection.withProcessingMetadata(asOf = Instant.now())), context) {
            doMutate(target.mutation!!, context, it.instance).first
               .map { typedInstance ->
                  typedInstance.withProcessingMetadata(asOf = it.processingStart)
               }
         }
      } else {
         /**
          * We are not projecting but mutation might require an implicit projection so hope onto projectionProvider coroutine context
          * so that we can perform the implicit projection concurrently.
          */
         if (target.projection == null) {
            projectionProvider
               .process(projectedResults, context)
               { flowOf(it) }
               .flatMapMerge(concurrency = Int.MAX_VALUE) {
                  doMutate(target.mutation!!, context, it.instance).first
                     .map { typedInstance ->
                        typedInstance.withProcessingMetadata(asOf = it.processingStart)
                     }
               }
         } else {
            /**
             * If we are projecting we are already on a LocalProjectionProvider context here, i.e. on one of the orbital_projection threads
             * so continue the mutation on the same thread.
             */
            projectedResults
               .flatMapMerge(concurrency = Int.MAX_VALUE) { queryResult ->
                  doMutate(target.mutation!!, context, queryResult.instance).first
                  .map { typedInstance ->
                     typedInstance.withProcessingMetadata(asOf = queryResult.processingStart)
                  }
            }
         }
      }
   }

   private fun performMutationProjection(
      context: QueryContext,
      mutationFlow: Flow<TypedInstanceWithMetadata>,
      mutation: Mutation?): Flow<TypedInstanceWithMetadata> {
      return mutation?.projectedType?.let { mutationProjectedType ->
         val factsToPropagate = initialState.toFactBag(schema)
         projectionProvider.project(
            mutationFlow,
            schema.type(mutation.operation.returnType),
            Projection(schema.type(mutationProjectedType.first), mutationProjectedType.second),
            context,
            factsToPropagate)

      } ?: mutationFlow
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


/**
 * Carries a typed instance, along with metadata captured during processing
 */
data class TypedInstanceWithMetadata(
   val processingStart: Instant,
   val instance: TypedInstance
)

fun TypedInstance.withProcessingMetadata(asOf: Instant = Instant.now()): TypedInstanceWithMetadata {
   return TypedInstanceWithMetadata(
      processingStart = asOf,
      this
   )
}
