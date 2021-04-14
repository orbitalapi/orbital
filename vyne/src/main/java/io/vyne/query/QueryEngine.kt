package io.vyne.query

import io.vyne.*
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.models.TypedObject
import io.vyne.query.active.ActiveQueryMonitor
import io.vyne.query.active.isQueryCancelled
import io.vyne.query.graph.EvaluatedEdge
import io.vyne.query.graph.operationInvocation.SearchRuntimeException
import io.vyne.schemas.Operation
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.utils.log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.stream.Collectors


open class SearchFailedException(
   message: String,
   val evaluatedPath: List<EvaluatedEdge>,
   val profilerOperation: ProfilerOperation
) : RuntimeException(message)

open class ProjectionFailedException(message: String) : RuntimeException(message)
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
      return QueryContext.from(schema, facts + additionalFacts, this, profiler, queryId = queryId)
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
      // EXPERIMENT: Detecting transforming of collections, ie A[] -> B[]
      // We're hacking this to to A.map{ build(B) }.
      // This could cause other issues, but I want to explore this approach

      val isCollectionToCollectionTransformation = context.facts.size == 1
         && context.facts.first() is TypedCollection
         && targetType.isCollection

      val isCollectionsToCollectionTransformation =
         context.facts.stream().allMatch { it is TypedCollection }
            && targetType.isCollection

      val isSingleToCollectionTransform = onlyTypedObject(context) != null
         && targetType.isCollection

      val querySpecTypeNode = QuerySpecTypeNode(targetType, emptySet(), QueryMode.DISCOVER)
      val result: TypedInstance? = when {
         //isCollectionToCollectionTransformation -> {
         //   mapCollectionToCollection(targetType, context)
         //}
         isCollectionsToCollectionTransformation -> {
            mapCollectionsToCollection(targetType, context)
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

      return if (result != null) {
         QueryResult(
            querySpecTypeNode,
            flow { emit(result) },
            emptySet(),
            profilerOperation = context.profiler.root,
            anonymousTypes = context.schema.typeCache.anonymousTypes(),
            queryId = context.queryId
         )
      } else {
         QueryResult(
            querySpecTypeNode,
            emptyFlow(),
            setOf(querySpecTypeNode),
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
   private suspend fun mapCollectionsToCollection(targetType: Type, context: QueryContext): TypedInstance? {
      val targetCollectionType = targetType.resolveAliases().typeParameters[0]
      log().info("Mapping collections to collection of type ${targetCollectionType.qualifiedName} ")
      val transformed = context.facts
         .map { it as TypedCollection }
         .flatten()
         .map { typedInstance -> mapTo(targetCollectionType, typedInstance, context) }
         .mapNotNull { it }
      return if (transformed.isEmpty()) {
         null
      } else {
         TypedCollection.from(transformed)
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
         val results = transformationResult.results?.toList()
         require(results?.size == 1) { "Expected only a single transformation result" }
         val result = results?.first()
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
         unmatchedNodes = queryResult.unmatchedNodes,
         path = null,
         profilerOperation = queryResult.profilerOperation,
         anonymousTypes = queryResult.anonymousTypes,
         queryId = context.queryId
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

      val resultsFlow = flow {
         var resultsReceivedFromStrategy = false

         for (queryStrategy in strategies) {
            if (resultsReceivedFromStrategy) {
               break
            }

            val strategyResult =
               invokeStrategy(context, queryStrategy, target, InvocationConstraints(spec, excludedOperations))
            if (strategyResult.hasMatchesNodes()) {
               strategyResult.matchedNodes?.collectIndexed {index,value ->
                  resultsReceivedFromStrategy = true
                  emit(value)

                  //Check the query state every 25 records
                  if (index % 25 == 0 && isQueryCancelled(context.queryId)) {
                     log().warn("Query ${context.queryId} cancelled - cancelling collection and publication of results")
                     currentCoroutineContext().cancel()
                  }

               }
            }
         }
      }


      // MP : We could possibly remove this line.
      // If we're not projecting, I suspect we use the return value from the query
      //rather than storing in the context and then fetching it back out again.
      // If we ARE projecting, then we construct a new context per list-element anyway.
      // However, this theory needs investigating / testing.

      // Note : We should add this additional data to the context too,
      // so that it's available for future query strategies to use.
      //context.addFacts(strategyResult.matchedNodes.values.filterNotNull())

      // isProjecting is a (maybe) temporary little fix to allow projection
      // Without it, there's a stack overflow error as projectTo seems to call ObjectBuilder.build which calls projectTo again.
      // ... Investigate


      val results = when(context.projectResultsTo) {
         null -> resultsFlow
         else ->
            resultsFlow.map {
               GlobalScope.async {
                  val actualProjectedType = context.projectResultsTo?.collectionType ?: context.projectResultsTo
                  val buildResult = context.only(it).build(actualProjectedType!!.qualifiedName)
                  buildResult.results.first()

               }
            }
               .buffer(64)
               .map { it.await() }

      }

      return QueryResult(
         when (context.projectResultsTo) {
            null -> target
            else -> QuerySpecTypeNode(context.projectResultsTo!!, emptySet(), QueryMode.DISCOVER)
         },
         results,
         emptySet(),
         path = null,
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

