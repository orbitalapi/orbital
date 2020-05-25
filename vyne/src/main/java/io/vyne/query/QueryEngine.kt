package io.vyne.query

import io.vyne.*
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.query.graph.EvaluatedEdge
import io.vyne.query.graph.operationInvocation.SearchRuntimeException
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.utils.log


open class SearchFailedException(message: String, val evaluatedPath: List<EvaluatedEdge>, val profilerOperation: ProfilerOperation) : RuntimeException(message)

interface QueryEngine {

   val schema: Schema
   fun find(type: Type, context: QueryContext): QueryResult
   fun find(queryString: QueryExpression, context: QueryContext): QueryResult
   fun find(target: QuerySpecTypeNode, context: QueryContext): QueryResult
   fun find(target: Set<QuerySpecTypeNode>, context: QueryContext): QueryResult

   fun gather(queryString: QueryExpression, context: QueryContext): QueryResult

   fun queryContext(
      factSetIds: Set<FactSetId> = setOf(FactSets.DEFAULT),
      additionalFacts: Set<TypedInstance> = emptySet(),
      resultMode: ResultMode = ResultMode.SIMPLE): QueryContext

   fun build(type: Type, context: QueryContext): QueryResult = build(TypeNameQueryExpression(type.fullyQualifiedName), context)
   fun build(query: QueryExpression, context: QueryContext): QueryResult

   fun parse(queryExpression: QueryExpression): Set<QuerySpecTypeNode>
}

/**
 * A query engine which allows for the provision of initial state
 */
class StatefulQueryEngine(
   initialState: FactSetMap,
   schema: Schema,
   strategies: List<QueryStrategy>,
   private val profiler: QueryProfiler = QueryProfiler()) :
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
      resultMode: ResultMode): QueryContext {
      val facts = this.factSets.filterFactSets(factSetIds).values().toSet()
      return QueryContext.from(schema, facts + additionalFacts, this, profiler, resultMode)
   }
}

// Note:  originally, there were two query engines (Default and Stateful), but only one was ever used (stateful).
// I've removed the default, and made it the BaseQueryEngine.  However, even this might be overkill, and we may
// fold this into a single class later.
// The separation between what's in the base and whats in the concrete impl. is not well thought out currently.
abstract class BaseQueryEngine(override val schema: Schema, private val strategies: List<QueryStrategy>) : QueryEngine {

   private val queryParser = QueryParser(schema)
   override fun gather(queryString: QueryExpression, context: QueryContext): QueryResult {
      // First pass impl.
      // Thinking here is that if I can add a new Hipster strategy that discovers all the
      // endpoints, then I can compose a result of gather() from multiple finds()
      val gatherQuery = queryParser.parse(queryString).map { it.copy(mode = QueryMode.GATHER) }.toSet()
      return find(gatherQuery, context)
   }

   // Experimental.
   // I'm starting this by treating find() and build() as seperate operations, but
   // I'm not sure why...just a gut feel.
   // The idea use case here is for ETL style transformations, where a user may know
   // some, but not all, of the facts up front, and then use Vyne to polyfill.
   // Build starts by using facts known in it's current context to build the target type
   override fun build(query: QueryExpression, context: QueryContext): QueryResult {
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

      // EXPERIMENT: Detecting transforming of collections, ie A[] -> B[]
      // We're hacking this to to A.map{ build(B) }.
      // This could cause other issues, but I want to explore this approach
      val isCollectionToCollectionTransformation = context.facts.size == 1
         && context.facts.first() is TypedCollection
         && targetType.isCollection

      val isCollectionsToCollectionTransformation =
         context.facts.stream().allMatch {it is TypedCollection}
         && targetType.isCollection

      val querySpecTypeNode = QuerySpecTypeNode(targetType, emptySet(), QueryMode.DISCOVER)
      val result: TypedInstance? = if (isCollectionToCollectionTransformation) {
         mapCollectionToCollection(targetType, context)
      } else if (isCollectionsToCollectionTransformation) {
         mapCollectionsToCollection(targetType, context)
      } else {
         ObjectBuilder(this, context).build(targetType)
      }

      return if (result != null) {
         QueryResult(
            mapOf(querySpecTypeNode to result),
            emptySet(),
            resultMode = context.resultMode,
            profilerOperation = context.profiler.root
         )
      } else {
         QueryResult(
            emptyMap(),
            setOf(querySpecTypeNode),
            resultMode = context.resultMode,
            profilerOperation = context.profiler.root
         )
      }
   }

   private fun mapCollectionsToCollection(targetType: Type, context: QueryContext): TypedInstance? {
      val targetCollectionType = targetType.resolveAliases().typeParameters[0]
      val transformed = context.facts
         .map { it as TypedCollection }
         .map { projectToAnotherType(context, it, targetCollectionType) }
         .mapNotNull { it }
      return TypedCollection.from(transformed);
   }

   private fun mapCollectionToCollection(targetType: Type, context: QueryContext): TypedInstance? {
      require(targetType.resolveAliases().typeParameters.size == 1) { "Expected collection type to contain exactly 1 parameter" }
      val collectionType = targetType.resolveAliases().typeParameters[0]

      val inboundFactList = (context.facts.first() as TypedCollection).value
      val transformed = inboundFactList.mapNotNull {
         projectToAnotherType(context, it, collectionType)
      }
      return TypedCollection.from(transformed);
   }

   private fun projectToAnotherType(context: QueryContext, typedInstance: TypedInstance, targetType: Type): TypedInstance? {
      val transformationResult = context.only(typedInstance).build(targetType.fullyQualifiedName)
      return if (transformationResult.isFullyResolved) {
         require(transformationResult.results.size == 1) { "Expected only a single transformation result" }
         val result = transformationResult.results.values.first()
         if (result == null) {
            log().warn("Transformation from $typedInstance to instance of ${targetType.fullyQualifiedName} was reported as sucessful, but result was null")
         }
         result
      } else {
         log().warn("Failed to transform from $typedInstance to instance of ${targetType.fullyQualifiedName}")
         null
      }
   }

   override fun parse(queryExpression: QueryExpression):Set<QuerySpecTypeNode> {
      return queryParser.parse(queryExpression)
   }
   override fun find(queryString: QueryExpression, context: QueryContext): QueryResult {
      val target = queryParser.parse(queryString)
      return find(target, context)
   }

   override fun find(type: Type, context: QueryContext): QueryResult {
      return find(TypeNameQueryExpression(type.fullyQualifiedName), context)
   }

   override fun find(target: QuerySpecTypeNode, context: QueryContext): QueryResult {
      return find(setOf(target), context)
   }

   override fun find(target: Set<QuerySpecTypeNode>, context: QueryContext): QueryResult {
      try {
         return doFind(target, context)
      } catch (e: Exception) {
         log().error("Search failed with exception:", e)
         throw SearchRuntimeException(e, context.profiler.root)
      }
   }


   private fun doFind(target: Set<QuerySpecTypeNode>, context: QueryContext): QueryResult {
      // TODO : BIG opportunity to optimize this by evaluating multiple querySpecNodes at once.
      // Which would allow us to be smarter about results we collect from rest calls.
      // Optimize later.
      val results = target.map { doFind(it, context) }
      val result = results.reduce { acc, queryResult ->
         QueryResult(
            results = acc.results + queryResult.results,
            unmatchedNodes = acc.unmatchedNodes + queryResult.unmatchedNodes,
            path = null,
            profilerOperation = queryResult.profilerOperation,
            resultMode = context.resultMode
         )
      }
      return result
   }

   private fun doFind(target: QuerySpecTypeNode, context: QueryContext): QueryResult {

      val matchedNodes = mutableMapOf<QuerySpecTypeNode, TypedInstance?>()

      // Note: We used to take a set<QuerySpecTypeNode>, but currently only take a single.
      // We'll likely re-optimize to take multiple, but for now wrap in a set.
      // This can (and should) change in the future
      val querySet = setOf(target)

      // This is cheating, probably.
      // We only resolve top-level nodes, rather than traverse deeply.
      fun unresolvedNodes(): List<QuerySpecTypeNode> {
         return querySet.filterNot { matchedNodes.containsKey(it) }
      }

      val strategyIterator: Iterator<QueryStrategy> = strategies.iterator()
      while (strategyIterator.hasNext() && unresolvedNodes().isNotEmpty()) {
         val queryStrategy = strategyIterator.next()
         val strategyResult = context.startChild(this, "Query with ${queryStrategy.javaClass.simpleName}", OperationType.GRAPH_TRAVERSAL) { op ->
            op.addContext("Search target", querySet.map { it.type.fullyQualifiedName })
            queryStrategy.invoke(setOf(target), context)
         }
         // Note : We should add this additional data to the context too,
         // so that it's available for future query strategies to use.
         context.addFacts(strategyResult.matchedNodes.values.filterNotNull())

         matchedNodes.putAll(strategyResult.matchedNodes)

         if (strategyResult.additionalData.isNotEmpty()) {
            // Note: Maybe we should only start re-querying if unresolvedNodes() has content
            log().debug("Discovered additional facts, adding to the context")
            context.addFacts(strategyResult.additionalData)
         }
      }
      if (unresolvedNodes().isNotEmpty()) {
         log().error("The following nodes weren't matched: ${unresolvedNodes().joinToString(", ")}")
      }

      //      TODO("Rebuild Path")
      return QueryResult(
         matchedNodes,
         unresolvedNodes().toSet(),
         path = null,
         profilerOperation = context.profiler.root,
         resultMode = context.resultMode)
   }
}

