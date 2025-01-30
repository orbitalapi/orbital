@file:Suppress("unused")

package com.orbitalhq.query.graph

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.orbitalhq.VyneCacheConfiguration
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.InvocationConstraints
import com.orbitalhq.query.QueryContext
import com.orbitalhq.query.QuerySpecTypeNode
import com.orbitalhq.query.QueryStrategy
import com.orbitalhq.query.QueryStrategyResult
import com.orbitalhq.query.SearchGraphExclusion
import com.orbitalhq.query.graph.edges.EvaluatableEdge
import com.orbitalhq.query.graph.edges.EvaluatedEdge
import com.orbitalhq.query.graph.edges.PathEvaluation
import com.orbitalhq.query.graph.edges.StartingEdge
import com.orbitalhq.schemas.Link
import com.orbitalhq.schemas.Path
import com.orbitalhq.schemas.Relationship
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.describe
import com.orbitalhq.utils.ImmutableEquality
import es.usc.citius.hipster.algorithm.Algorithm
import es.usc.citius.hipster.graph.HipsterDirectedGraph
import es.usc.citius.hipster.model.impl.WeightedNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import lang.taxi.services.operations.constraints.Constraint
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}


private data class SearchPathExclusionKey(val startInstanceType: TypedInstance, val target: Element) {
   private val equality =
      ImmutableEquality(this, SearchPathExclusionKey::startInstanceType, SearchPathExclusionKey::target)

   override fun equals(other: Any?): Boolean = equality.isEqualTo(other)
   override fun hashCode(): Int = equality.hash()
}

private data class StrategyInvocationCacheKey(
   val context: QueryContext,
   val target: QuerySpecTypeNode,
   val invocationConstraints: InvocationConstraints,
   val coroutineContext: CoroutineScope,
) {
   val facts = context.rootAndScopedFacts()
   private val equality = ImmutableEquality(
      this,
      StrategyInvocationCacheKey::facts,
      StrategyInvocationCacheKey::target
   )

   override fun hashCode(): Int {
      return equality.hash()
   }

   override fun equals(other: Any?): Boolean {
      return equality.isEqualTo(other)
   }
}

class GraphSearchQueryStrategy(
   private val edgeEvaluator: EdgeNavigator,
   vyneCacheConfigration: VyneCacheConfiguration
) : QueryStrategy {

   private val searchPathExclusionsCacheSize =
      vyneCacheConfigration.vyneDiscoverGraphQuery.searchPathExclusionsCacheSize

   private val schemaGraphCache = CacheBuilder.newBuilder()
      .maximumSize(vyneCacheConfigration.vyneDiscoverGraphQuery.schemaGraphCacheSize) // arbitary cache size, we can explore tuning this later
      .weakKeys()
      .build(object : CacheLoader<Schema, VyneGraphBuilder>() {
         override fun load(schema: Schema): VyneGraphBuilder {
            return VyneGraphBuilder(schema, vyneCacheConfigration.vyneGraphBuilderCache)
         }

      })

   private val searchPathExclusions = CacheBuilder
      .newBuilder()
      .maximumSize(searchPathExclusionsCacheSize.toLong())
      .weakKeys()
      .build<SearchPathExclusionKey, SearchPathExclusionKey>()
      .asMap()

   private val searchExecutingCacheLoaderx =
      object : CacheLoader<StrategyInvocationCacheKey, Deferred<QueryStrategyResult>>() {
         override fun load(key: StrategyInvocationCacheKey): Deferred<QueryStrategyResult> {

            return key.coroutineContext.async {
               logger.debug { "Invoking search for type ${key.target.type.qualifiedName.shortDisplayName} with ${key.facts.size} provided facts: ${key.facts.joinToString { it.type.qualifiedName.shortDisplayName }}" }

               val existingFacts = key.facts.filter { it.type == key.target.type }
               if (existingFacts.isNotEmpty()) {
                  logger.warn { "Attempting a graph search for type ${key.target.type.name.shortDisplayName} however an instance was already present in the provided facts." }
               }

               val context = key.context
               if (context.facts.isEmpty()) {
                  logger.debug { "[${context.queryId}] Cannot perform a graph search, as no facts provided to serve as starting point. " }
                  return@async QueryStrategyResult.searchFailed()
               }

               val targetElement = type(key.target.type)

               // search from every fact in the context
               find(targetElement, context, key.invocationConstraints)
            }
         }
      }

//   private val invocationCache: LoadingCache<StrategyInvocationCacheKey, Deferred<QueryStrategyResult>> = CacheBuilder
//      .newBuilder()
//      .maximumSize(vyneCacheConfigration.vyneDiscoverGraphQuery.invocationCacheSize)
//      .weakKeys()
//      .build(searchExecutingCacheLoader)

   override suspend fun invoke(
      target: Set<QuerySpecTypeNode>,
      context: QueryContext,
      invocationConstraints: InvocationConstraints
   ): QueryStrategyResult {
      if (target.size != 1) TODO("Support for target sets not yet built")
      val firstTarget = target.first()

      // MP: 14-May-23: This used to call to an invocationCache with the below cache key.
      // However, this resulted in a cache miss every time, so removing to try to simplify the call flow here,
      // which is on the hot path and has high CPU issues.
//      val cacheKey = StrategyInvocationCacheKey(
//         context = context,
//         target = firstTarget,
//         invocationConstraints,
//         CoroutineScope(currentCoroutineContext())
//      )
      val facts = context.rootAndScopedFacts()
      logger.debug { "Invoking search for type ${firstTarget.type.qualifiedName.shortDisplayName} with ${facts.size} provided facts: ${facts.joinToString { it.type.qualifiedName.shortDisplayName }}" }

      val existingFacts = facts.filter { it.type == firstTarget.type }
      if (existingFacts.isNotEmpty()) {
         logger.warn { "Attempting a graph search for type ${firstTarget.type.name.shortDisplayName} however an instance was already present in the provided facts." }
      }

      if (context.facts.isEmpty()) {
         logger.debug { "[${context.queryId}] Cannot perform a graph search, as no facts provided to serve as starting point. " }
         return QueryStrategyResult.searchFailed()
      }

      val targetElement = type(firstTarget.type)

      // Don't bother searching for a type, if the type isn't resolvable from a service.
      // This is specifically intended to prevent searching for projection types.
      // If we skip the search for the projection type, we'll then move into the individual fields, which
      // are discoverable.
      val allTypesFindableFromServices = context.schema.services.flatMap { it.remoteOperations }
         .flatMap { it.returnType.allReferencedTypes }
         .toSet()
      if (!allTypesFindableFromServices.contains(firstTarget.type)) {
         return QueryStrategyResult.searchFailed()
      }

      // If the search target specifies a constraint,
      // we can only consider the operations that advertise they satisfy that constraint.
      val invocationConstraintsForSearch = if (firstTarget.dataConstraints.isNotEmpty()) {
         val operationsReturningType = context.schema.remoteOperations
            .filter { it.returnType.isAssignableTo(firstTarget.type) }
         val operationsNotSatisfyingContract = operationsReturningType.filterNot { operation ->
            operation.contract.satisfiesAll(firstTarget.dataConstraints)
         }.map { SearchGraphExclusion("Does not satisfy requested contract", it) }
         invocationConstraints.copy(excludedOperations = invocationConstraints.excludedOperations + operationsNotSatisfyingContract)
      } else {
         invocationConstraints
      }

      // search from every fact in the context
      return find(targetElement, context, invocationConstraintsForSearch)
   }


   internal suspend fun find(
      targetElement: Element,
      context: QueryContext,
      invocationConstraints: InvocationConstraints
   ): QueryStrategyResult {
      val failedAttempts = mutableListOf<DataSource>()
      val rootScopedFacts = context.rootAndScopedFacts()


      val returnValue = internalFind(rootScopedFacts, targetElement, context, invocationConstraints, failedAttempts)
//      val returnValue = rootScopedFacts
//         .asFlow()
//         .mapNotNull { fact ->
//            internalFind(rootScopedFacts, targetElement, context, invocationConstraints, failedAttempts)
//         }
//         .firstOrNull()

      return if (returnValue != null) {
         if (!returnValue.type.isAssignableTo(targetElement.valueAsQualifiedName())) {
            logger.warn { "Return value isn't assignable to the requested type - expected ${targetElement.valueAsQualifiedName().shortDisplayName} but got ${returnValue.type.qualifiedName.shortDisplayName}" }
         }
         QueryStrategyResult.from(returnValue, failedAttempts)
      } else {
         QueryStrategyResult.searchFailed(failedAttempts)
      }
   }

   private suspend fun internalFind(
      startFacts: List<TypedInstance>,
//      fact: TypedInstance,
      targetElement: Element,
      context: QueryContext,
      invocationConstraints: InvocationConstraints,
      failedAttempts: MutableList<DataSource>
   ): TypedInstance? {
      // TODO :
      // We no longer iterate and search for each fact.
      // So, fact exclusion needs to be done within the searcher as paths fail.,
      // I think this is already handled, but need to be sure.
//      val factIndex = rootScopedFacts.indexOf(fact)
//      val startFact = providedStartFact(fact.type, factIndex)
//      val targetType = targetElement.instanceValue as? Type? ?: context.schema.type(targetElement.value as String)
//      // Excluding paths is done by the type, not the fact.
//      // Graph searches work based off of links from types, therefore
//      // we should exclude based on the type, regardless of the value.
//      val exclusionKey = SearchPathExclusionKey(fact, targetElement)
//      if (searchPathExclusions.contains(exclusionKey)) {
//         // if  a previous search for given (searchNode, targetNode) yielded 'null' path, then
//         // don't search.
//         return null
//      }
      val targetType = targetElement.instanceValue as? Type? ?: context.schema.type(targetElement.value as String)
      var searchProvidedAtLeastOnePath = false
      val searcher = GraphSearcher(
         targetElement,
         targetType,
         schemaGraphCache.get(context.schema),
         invocationConstraints
      )
      val evaluatedPathTempMap = mutableListOf<PathEvaluation>()
      val searchResult = searcher.search(
         context.rootAndScopedFacts(),
         context.excludedServices.toSet(),
         invocationConstraints.excludedOperations.plus(context.excludedOperations.map {
            SearchGraphExclusion("@Id", it)
         }),
         context.queryId
      )
      { pathToEvaluate ->
         searchProvidedAtLeastOnePath = true
         val evaluations = evaluatePath(pathToEvaluate, context, GraphSearcher.STARTING_ELEMENT, startFacts)
         evaluatedPathTempMap.addAll(evaluations)
         evaluations
      }
      // Only exclude if the pair of (searchNode, targetNode) didn't provide any paths at all.
      // It's possible that the search failed, but the path is valid to be considered again.
      // (eg., if we used a TypedInstance as an input to a service, but the service returned no results,
      // we shouldn't exclude the type, as future TypedInstances might have better luck)
//      if (searchPathExclusionsCacheSize > 0 && searchResult.path == null && !searchProvidedAtLeastOnePath) {
//         searchPathExclusions[exclusionKey] = exclusionKey
//      }


      // Consider the case we try to populate 'Director birthday' for a given movie.
      // a movie has a director name value
      // and the following path is discovered
      // Step 1: with director name invoke nameToDirectorId service that returns Director identifier  value
      // Step 2: with director identifier invoke Director service that returns DirectorData
      // Step 3: extract 'Director birthday' from DirectorData's name birthday attribute
      // Assume that nameToDirectorId service returns:
      //  {  name: 'Passolini', directorIdentifier: null }
      // so at the end of 'step 1p above typed object is inserted into context.facts
      // Step 2 will fail as 'directorId' is null (see RestTemplateInvoker line 113 for the point of exception)
      // As part of the search, the result of 'Step 1' i.e. {  name: 'Passolini', directorIdentifier: null }  will be reused as the starting fact
      // and we'll try to execute Step 1, Step 2 and Step 3 again
      // Step 1 will produce another {  name: 'Passolini', directorIdentifier: null } which will be added into context.facts
      // Step 2 will fail, but the fact added in the previous step will be used as another start point
      // which leads to an infinite loop
      // so here we try to avoid that.
//      if (searchPathExclusionsCacheSize > 0 && searchResult.path == null && evaluatedPathTempMap.isNotEmpty()) {
//         val duplicatedFact = evaluatedPathTempMap
//            .filter { it is EvaluatedEdge && it.edge.vertex1.elementType == ElementType.OPERATION && it.resultValue == fact }
//            .map { it.resultValue }
//            .firstOrNull()
//         if (duplicatedFact != null) {
//            logger.info { "[${context.queryId}] duplicate $duplicatedFact" }
//            searchPathExclusions[exclusionKey] = exclusionKey
//         }
//      }
      failedAttempts.addAll(searchResult.failedAttemptSources)
      return searchResult.typedInstance
   }

   private suspend fun evaluatePath(
      searchResult: WeightedNode<Relationship, Element, Double>,
      queryContext: QueryContext,
      startFact: Element,
      startFacts: List<TypedInstance>
   ): List<PathEvaluation> {
      // The actual result of this isn't directly used.  But the queryContext is updated with
      // nodes as they're discovered (eg., through service invocation)
      val evaluatedEdges = mutableListOf<PathEvaluation>(
         getStartingEdge(startFact, startFacts)
      )

      val path = searchResult.path()
      path
         .drop(1)
         .asSequence()
         .takeWhile {
            val lastEvaluation = evaluatedEdges.last()
            // Take as long as the last evaluation we made was successful.  Otherwise, stop.
            if (lastEvaluation is EvaluatedEdge) {
               lastEvaluation.wasSuccessful
            } else {
               true
            }
         }
         .mapIndexedTo(evaluatedEdges) { index, weightedNode ->
            // Note re index:  We dropped 1, so indexes are out-by-one.
            // Normally the lastValue would be index-1, but here, it's just index.
            val lastResult = evaluatedEdges[index]
            val endNode = weightedNode.state()
            val evaluatableEdge = EvaluatableEdge(lastResult, weightedNode.action(), endNode)
            if (evaluatableEdge.relationship == Relationship.PROVIDES) {
               logger.debug {
                  "As part of search ${path[0].state().value} -> ${
                     path.last().state().value
                  }, ${evaluatableEdge.vertex1.value} will be tried"
               }
            }

            val evaluationResult =
               edgeEvaluator.evaluate(evaluatableEdge, queryContext)
            if (evaluatableEdge.relationship == Relationship.PROVIDES) {
               logger.debug {
                  "As part of search ${path[0].state().value} -> ${
                     path.last().state().value
                  }, ${evaluatableEdge.vertex1.value} was executed. Successful : ${evaluationResult.wasSuccessful}"
               }
            }
            evaluationResult
         }

      return evaluatedEdges
   }

   private fun getStartingEdge(
      startFact: Element,
      startFactTypedInstances: List<TypedInstance>
   ): StartingEdge {
      return StartingEdge(TypedCollection.from(startFactTypedInstances), startFact)
   }
}

private fun List<WeightedNode<Relationship, Element, Double>>.toLinks(): List<Link> {
   return this.mapIndexed { index, _ ->
      if (index == 0) {
         null
      } else {
         val fromElement = this[index - 1].state()
         val toElement = this[index].state()
         val action = this[index].action()
         Link(fromElement.valueAsQualifiedName(), action, toElement.valueAsQualifiedName(), this[index].cost.toInt())
      }
   }.toList().filterNotNull()
}

private fun List<WeightedNode<Relationship, Element, Double>>.describe(): String {
   return this.toLinks().describe()
}

private fun List<WeightedNode<Relationship, Element, Double>>.describeLinks(): List<String> {
   return this.toLinks().map { it.toString() }
}

private fun Algorithm<*, Element, *>.SearchResult.recreatePath(
   start: Element,
   target: Element,
   graph: HipsterDirectedGraph<Element, Relationship>
): Path {
   val path = this.getOptimalPaths()[0]
   val links = path.mapIndexed { index, vertex ->
      if (index + 1 >= path.size) {
         null
      } else {
         val fromElement = vertex
         val toElement = path[index + 1]
         val edge = graph.outgoingEdgesOf(fromElement).firstOrNull { it.vertex2 == toElement }
            ?: throw IllegalStateException("No edge found from $fromElement -> $toElement, but they were adjoining nodes in the result")
         Link(fromElement.valueAsQualifiedName(), edge.edgeValue, toElement.valueAsQualifiedName())
      }
   }.filterNotNull()
   return Path(start.valueAsQualifiedName(), target.valueAsQualifiedName(), links)
}

fun <V, E> HipsterDirectedGraph<V, E>.edgeDescriptions(): List<String> {
   return this.vertices().flatMap {
      this.outgoingEdgesOf(it).map { edge ->
         "${edge.vertex1} -[${edge.edgeValue}]-> ${edge.vertex2}"
      }
   }
}


