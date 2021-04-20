package io.vyne.query

import io.vyne.*
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.models.TypedObject
import io.vyne.query.active.isQueryCancelled
import io.vyne.query.graph.EvaluatedEdge
import io.vyne.query.graph.operationInvocation.SearchRuntimeException
import io.vyne.schemas.Operation
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.utils.log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.*
import java.util.stream.Collectors


open class SearchFailedException(
   message: String,
   val evaluatedPath: List<EvaluatedEdge>,
   val profilerOperation: ProfilerOperation
) : RuntimeException(message)

interface QueryEngine {

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
      clientQueryId: String?
   ): QueryContext

   suspend fun build(type: Type, context: QueryContext): QueryResult =
      build(TypeNameQueryExpression(type.fullyQualifiedName), context)

   suspend fun build(query: QueryExpression, context: QueryContext): QueryResult

   fun parse(queryExpression: QueryExpression): Set<QuerySpecTypeNode>
}

/**
 * A query engine which allows for the provision of initial state
 */
class StatefulQueryEngine(
   initialState: FactSetMap,
   schema: Schema,
   strategies: List<QueryStrategy>,
   private val profiler: QueryProfiler = QueryProfiler()
) :
   BaseQueryEngine(schema, strategies), ModelContainer {
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
      clientQueryId: String?
   ): QueryContext {
      val facts = this.factSets.filterFactSets(factSetIds).values().toSet()
      return QueryContext.from(
         schema,
         facts + additionalFacts,
         this,
         profiler,
         queryId = queryId,
         clientQueryId = clientQueryId
      )
   }

}

// Note:  originally, there were two query engines (Default and Stateful), but only one was ever used (stateful).
// I've removed the default, and made it the BaseQueryEngine.  However, even this might be overkill, and we may
// fold this into a single class later.
// The separation between what's in the base and whats in the concrete impl. is not well thought out currently.
abstract class BaseQueryEngine(override val schema: Schema, private val strategies: List<QueryStrategy>) : QueryEngine {

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
            ObjectBuilder(this, context, targetType).build()
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
            queryId = context.queryId
         )
      } else {
         QueryResult(
            querySpecTypeNode,
            emptyFlow(),
            isFullyResolved = false,
            profilerOperation = context.profiler.root,
            queryId = context.queryId,
            clientQueryId = context.clientQueryId,
            anonymousTypes = context.schema.typeCache.anonymousTypes()
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

   // This logic executes currently as part of projection from one collection to another
   /*
   private suspend fun mapCollectionToCollection(targetType: Type, context: QueryContext): TypedInstance? {
      require(targetType.resolveAliases().typeParameters.size == 1) { "Expected collection type to contain exactly 1 parameter" }
      val targetCollectionType = targetType.resolveAliases().typeParameters[0]
      return timed("QueryEngine.mapTo ${targetCollectionType.qualifiedName}") {
         val inboundFactList = (context.facts.first() as TypedCollection).value
         log().info("Mapping TypedCollection.size=${inboundFactList.size} to ${targetCollectionType.qualifiedName} ")
         val transformed = inboundFactList.mapNotNull { it -> mapTo(targetCollectionType, it, context) }
         return@timed when {
            transformed.size == 1 && transformed.first()?.type?.isCollection == true -> TypedCollection.from((transformed.first()!! as TypedCollection).value)
            else -> TypedCollection.from(flattenResult(transformed))
         }
      }
   }

    */

   /*
   private suspend fun mapSingleToCollection(targetType: Type, context: QueryContext): TypedInstance? {
      require(targetType.resolveAliases().typeParameters.size == 1) { "Expected collection type to contain exactly 1 parameter" }
      val targetCollectionType = targetType.resolveAliases().typeParameters[0]
      //return timed("QueryEngine.mapTo ${targetCollectionType.qualifiedName}") {

         val inboundFactList = listOf(onlyTypedObject(context)!!)
         log().info("Mapping TypedCollection.size=${inboundFactList.size} to ${targetCollectionType.qualifiedName} ")
         val transformed = inboundFactList
            .stream()
            .map {
               mapTo(targetCollectionType, it, context)

            }
            .filter { it != null }
            .collect(Collectors.toList())

         return when {
            transformed.size == 1 && transformed.first()?.type?.isCollection == true -> TypedCollection.from((transformed.first()!! as TypedCollection).value)
            else -> TypedCollection.from(flattenResult(transformed))
         }
      //}
   }

    */

   private fun flattenResult(result: List<TypedInstance?>): List<TypedInstance> {
      return result
         .filterNotNull()
         .flatMap {
            when (it) {
               is TypedCollection -> it.value
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
      return find(TypeNameQueryExpression(type.fullyQualifiedName), context, spec)
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
         clientQueryId = context.clientQueryId
      )

   }

   private fun doFind(
      target: QuerySpecTypeNode,
      context: QueryContext,
      spec: TypedInstanceValidPredicate,
      excludedOperations: Set<SearchGraphExclusion<Operation>> = emptySet()
   ): QueryResult {

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
      val resultsFlow: Flow<TypedInstance> = flow {
         for (queryStrategy in strategies) {
            if (resultsReceivedFromStrategy) {
               break
            }

            val strategyResult =
               invokeStrategy(context, queryStrategy, target, InvocationConstraints(spec, excludedOperations))

            if (strategyResult.hasMatchesNodes()) {
               strategyResult.matchedNodes.collectIndexed { index, value ->
                  resultsReceivedFromStrategy = true
                  emit(value)

                  //Check the query state every 25 records
                  // TODO : This needs to be refactored, as currently
                  // relates on global shared state.
                  // We need to hold running queries in the query-server,
                  // and send a cancellation flag / signal down through
                  // the queryContext.
                  if (index % 25 == 0 && isQueryCancelled(context.queryId)) {
                     log().warn("Query ${context.queryId} cancelled - cancelling collection and publication of results")
                     currentCoroutineContext().cancel()
                  }

               }
            } else {
               log().debug("Strategy ${queryStrategy::class.simpleName} failed to resolve ${target.description}")
            }
         }
         if (!resultsReceivedFromStrategy) {
            val constraintsSuffix = if (target.dataConstraints.isNotEmpty()) {
               "with the ${target.dataConstraints.size} constraints provided"
            } else ""
            throw SearchFailedException("No strategy found for discovering type ${target.description} $constraintsSuffix".trim(), emptyList(), context)
         }
      }

      val results = when (context.projectResultsTo) {
         null -> resultsFlow
         else ->
            // This pattern aims to allow the concurrent execution of multiple flows.
            // Normally, flow execution is sequential - ie., one flow must complete befre the next
            // item is taken.  buffer() is used here to allow up to n parallel flows to execute.
            // MP: @Anthony - please leave some comments here that describe the rationale for
            // map { async { .. } }.flatMapMerge { await }
            resultsFlow.map {
               GlobalScope.async {
                  val actualProjectedType = context.projectResultsTo?.collectionType ?: context.projectResultsTo
                  val buildResult = context.only(it).build(actualProjectedType!!.qualifiedName)
                  buildResult.results
               }
            }
            .buffer(128)
            .flatMapMerge { it.await() }

      }
      val querySpecTypeNode = if (context.projectResultsTo != null) {
         QuerySpecTypeNode(context.projectResultsTo!!, emptySet(), QueryMode.DISCOVER)
      } else {
         target
      }
      return QueryResult(
         querySpecTypeNode,
         results,

         isFullyResolved = true,
         profilerOperation = context.profiler.root,
         queryId = context.queryId,
         clientQueryId = context.clientQueryId,
         anonymousTypes = context.schema.typeCache.anonymousTypes()
      )

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

