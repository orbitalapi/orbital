@file:Suppress("unused")

package io.vyne.query.graph

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import es.usc.citius.hipster.algorithm.Algorithm
import es.usc.citius.hipster.graph.HipsterDirectedGraph
import es.usc.citius.hipster.model.impl.WeightedNode
import io.vyne.VyneCacheConfiguration
import io.vyne.models.DataSource
import io.vyne.models.TypedInstance
import io.vyne.query.*
import io.vyne.query.graph.edges.EvaluatableEdge
import io.vyne.query.graph.edges.EvaluatedEdge
import io.vyne.query.graph.edges.PathEvaluation
import io.vyne.query.graph.edges.StartingEdge
import io.vyne.schemas.*
import io.vyne.utils.ImmutableEquality
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.*
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
   val facts = context.facts.rootAndScopedFacts()
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

class HipsterDiscoverGraphQueryStrategy(
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
      .build<SearchPathExclusionKey, SearchPathExclusionKey>()
      .asMap()

   private val searchExecutingCacheLoader =
      object : CacheLoader<StrategyInvocationCacheKey, Deferred<QueryStrategyResult>>() {
         override fun load(key: StrategyInvocationCacheKey): Deferred<QueryStrategyResult> {
            return key.coroutineContext.async {
               logger.info { "Invoking search for type ${key.target.type.qualifiedName.shortDisplayName} with ${key.facts.size} provided facts" }

               // MP: 23-Sep-22: We have removed the concept of findOne / findAll, and now only support find.
               // Having made that change, EsgTest started failing, as the below wasn't being invoked.
               // Not sure why we didn't use GraphSearch when doing DISCOVER (which related to findOne).
               //
               // Have excluded this, as I suspect the check is legacy.
               // All tests passed after I did that, but...well... I'm worried.
               // If stuff starts breaking, this could be the cause.
//      if (firstTarget.mode != QueryMode.DISCOVER) {
//         return QueryStrategyResult.searchFailed()
//      }
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

   private val invocationCache: LoadingCache<StrategyInvocationCacheKey, Deferred<QueryStrategyResult>> = CacheBuilder
      .newBuilder()
      .build(searchExecutingCacheLoader)

   override suspend fun invoke(
      target: Set<QuerySpecTypeNode>,
      context: QueryContext,
      invocationConstraints: InvocationConstraints
   ): QueryStrategyResult {
      if (target.size != 1) TODO("Support for target sets not yet built")
      val firstTarget = target.first()

      val cacheKey = StrategyInvocationCacheKey(
         context = context,
         target = firstTarget,
         invocationConstraints,
         CoroutineScope(currentCoroutineContext())
      )

//      return invocationCache.get(cacheKey).await()
      if (context.facts.isEmpty()) {
         logger.debug { "[${context.queryId}] Cannot perform a graph search, as no facts provided to serve as starting point. " }
         return QueryStrategyResult.searchFailed()
      }

      val targetElement = type(firstTarget.type)

      // search from every fact in the context
      return find(targetElement, context, invocationConstraints)
   }


   internal suspend fun find(
      targetElement: Element,
      context: QueryContext,
      invocationConstraints: InvocationConstraints
   ): QueryStrategyResult {
      val failedAttempts = mutableListOf<DataSource>()
      val ret = context.facts
         .asFlow()

         //    .filter { it is TypedObject }
         .mapNotNull { fact ->
            val startFact = providedInstance(fact)
            val targetType = context.schema.type(targetElement.value as String)
            // Excluding paths is done by the type, not the fact.
            // Graph searches work based off of links from types, therefore
            // we should exclude based on the type, regardless of the value.
            val exclusionKey = SearchPathExclusionKey(fact, targetElement)
            if (searchPathExclusions.contains(exclusionKey)) {
               // if  a previous search for given (searchNode, targetNode) yielded 'null' path, then
               // don't search.
               return@mapNotNull null
            }
            var searchProvidedAtLeastOnePath = false
            val searcher = GraphSearcher(
               startFact,
               targetElement,
               targetType,
               schemaGraphCache.get(context.schema),
               invocationConstraints
            )
            val evaluatedPathTempMap = mutableListOf<PathEvaluation>()
            val searchResult = searcher.search(
               context.facts,
               context.excludedServices.toSet(),
               invocationConstraints.excludedOperations.plus(context.excludedOperations.map {
                  SearchGraphExclusion(
                     "@Id",
                     it
                  )
               }),
               context.queryId
            )
            { pathToEvaluate ->
               searchProvidedAtLeastOnePath = true
               val evaluations = evaluatePath(pathToEvaluate, context, startFact)
               evaluatedPathTempMap.addAll(evaluations)
               evaluations
            }
            // Only exclude if the pair of (searchNode, targetNode) didn't provide any paths at all.
            // It's possible that the search failed, but the path is valid to be considered again.
            // (eg., if we used a TypedInstance as an input to a service, but the service returned no results,
            // we shouldn't exclude the type, as future TypedInstances might have better luck)
            if (searchPathExclusionsCacheSize > 0 && searchResult.path == null && !searchProvidedAtLeastOnePath) {
               searchPathExclusions[exclusionKey] = exclusionKey
            }


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
            if (searchPathExclusionsCacheSize > 0 && searchResult.path == null && evaluatedPathTempMap.isNotEmpty()) {
               val duplicatedFact = evaluatedPathTempMap
                  .filter { it is EvaluatedEdge && it.edge.vertex1.elementType == ElementType.OPERATION && it.resultValue == fact }
                  .map { it.resultValue }
                  .firstOrNull()
               if (duplicatedFact != null) {
                  logger.info { "[${context.queryId}] duplicate $duplicatedFact" }
                  searchPathExclusions[exclusionKey] = exclusionKey
               }
            }
            failedAttempts.addAll(searchResult.failedAttemptSources)
            searchResult.typedInstance
         }
         .firstOrNull()

      return if (ret != null) {
         QueryStrategyResult.from(ret, failedAttempts)
      } else {
         QueryStrategyResult.searchFailed(failedAttempts)
      }
   }

   private suspend fun evaluatePath(
      searchResult: WeightedNode<Relationship, Element, Double>,
      queryContext: QueryContext,
      startFact: Element
   ): List<PathEvaluation> {
      // The actual result of this isn't directly used.  But the queryContext is updated with
      // nodes as they're discovered (eg., through service invocation)
      val evaluatedEdges = mutableListOf<PathEvaluation>(
         getStartingEdge(startFact)
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
      startFact: Element
   ): StartingEdge {
      return StartingEdge(startFact.instanceValue as TypedInstance, startFact)
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


