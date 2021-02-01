package io.vyne.query

import com.google.common.base.Stopwatch
import es.usc.citius.hipster.algorithm.Hipster
import es.usc.citius.hipster.graph.HipsterDirectedGraph
import es.usc.citius.hipster.model.Transition
import es.usc.citius.hipster.model.impl.WeightedNode
import es.usc.citius.hipster.model.problem.ProblemBuilder
import io.vyne.models.TypedInstance
import io.vyne.query.SearchResult.Companion.noPath
import io.vyne.query.SearchResult.Companion.noResult
import io.vyne.query.graph.Element
import io.vyne.query.graph.EvaluatableEdge
import io.vyne.query.graph.EvaluatedEdge
import io.vyne.query.graph.PathEvaluation
import io.vyne.query.graph.VyneGraphBuilder
import io.vyne.schemas.Operation
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Relationship
import io.vyne.schemas.Type
import io.vyne.utils.log
import java.util.concurrent.TimeUnit

// This class is not optimized.  Need to investigate how to speed it up.
class GraphSearcher(
   private val startFact: Element,
   private val targetFact: Element,
   private val targetType: Type,
   private val graphBuilder: VyneGraphBuilder,
   private val invocationConstraints: InvocationConstraints
) {

   private val graphBuilderTimes = mutableListOf<Long>()
   private val graphSearchTimes = mutableListOf<Long>()
   private val pathExclusionCalculator = PathExclusionCalculator()

   companion object {
      const val MAX_SEARCH_COUNT = 100
   }

   private enum class PathPrevaliationResult {
      EVALUATE,

      // We made some changes to the graph, so try again
      REQUERY,

      // Don't attempt this, but no point in requerying, as the graph is unchanged
      ABORT
   }

   private fun prevalidatePath(
      proposedPath: WeightedNode<Relationship, Element, Double>,
      excludedEdges: MutableList<EvaluatableEdge>
   ): PathPrevaliationResult {
      // TODO
      return PathPrevaliationResult.EVALUATE
   }

   fun search(
      knownFacts: Set<TypedInstance>,
      excludedServices: Set<SearchGraphExclusion<QualifiedName>>,
      excludedOperations: Set<SearchGraphExclusion<Operation>>,
      evaluator: PathEvaluator
   ): SearchResult {
      // TODO : EEEK!  We should be adding the instances, not the types.
      // This will cause problems when we have multiple facts of the same type,
      // as one may result in a happy path, and the other might not.
//      val factTypes = knownFacts.map { it.type }.toSet()

      val excludedOperationsNames = excludedOperations.map { it.excludedValue.qualifiedName }.toSet()
      val excludedInstance = mutableSetOf<TypedInstance>()
      val excludedEdges = mutableListOf<EvaluatableEdge>()
      val evaluatedPaths = EvaluatedPathSet()

      tailrec fun buildNextPath(): WeightedNode<Relationship, Element, Double>? {
         val facts = if (excludedInstance.isEmpty()) {
            knownFacts
         } else {
            knownFacts.filterNot { excludedInstance.contains(it) }
         }
         // Note: I think we can migrate to using exclusively excludedEdges (Not using excludedOperations
         // and excludedInstances)..as it should be a more powerful abstraction
         val proposedPath =
            findPath(facts, excludedOperationsNames, excludedEdges, excludedServices.excludedValues(), evaluatedPaths)
         return when {
            proposedPath == null -> null
            evaluatedPaths.containsPath(proposedPath) -> {
               log().debug("The proposed path with id ${proposedPath.pathHashExcludingWeights()} has already been evaluated, so will not be tried again.")
               null
            }
            else -> {
               when (prevalidatePath(proposedPath, excludedEdges)) {
                  PathPrevaliationResult.EVALUATE -> proposedPath
                  PathPrevaliationResult.REQUERY -> buildNextPath()
                  PathPrevaliationResult.ABORT -> null
               }
            }
         }
      }

      var searchCount = 0
      var nextPath = buildNextPath()
      while (nextPath != null) {
         evaluatedPaths.addPath(nextPath)
         searchCount++
         if (searchCount > MAX_SEARCH_COUNT) {
            log().error("Search iterations exceeded max count. Stopping, lest we search forever in vein")
            return noResult(nextPath)
         }
         val evaluatedPath = evaluator(nextPath)
         val (pathEvaluatedSuccessfully, resultValue) = wasSuccessful(evaluatedPath)
         val resultSatisfiesConstraints =
            pathEvaluatedSuccessfully && invocationConstraints.typedInstanceValidPredicate.isValid(resultValue)
         if (pathEvaluatedSuccessfully && resultSatisfiesConstraints) {
            return SearchResult(resultValue, nextPath)
         } else {
            appendIgnorableEdges(evaluatedPath, excludedEdges)
            nextPath = buildNextPath()
         }
      }
      // There were no search paths to evaluate.  Just exit
      log().info("Failed to find path from ${startFact.label()} to ${targetFact.label()} after $searchCount searches")
      return noPath()
   }

   private fun appendIgnorableEdges(
      evaluatedPath: List<PathEvaluation>,
      excludedEdges: MutableList<EvaluatableEdge>
   ): Boolean {
      val edgesToExclude = pathExclusionCalculator.findEdgesToExclude(evaluatedPath, invocationConstraints)
      if (edgesToExclude.size > 1) {
         log().warn("Found ${edgesToExclude.size} edges to exclude.  Currently, that's unexpected, but not neccessarily wrong.  This should be investigated")
      }
      return if (edgesToExclude.isNotEmpty()) {
         excludedEdges.addAll(edgesToExclude)
         true
      } else {
         false
      }
   }

   private fun logOperationCost() {
      val buildCost = graphBuilderTimes.sum()
      val searchCost = graphSearchTimes.sum()
      val totalCost = buildCost + searchCost
      log().info("Graph search took $totalCost ms, ${buildCost}ms in ${graphBuilderTimes.size} build operations, and ${searchCost}ms in ${graphSearchTimes.size} searches")
   }

   private fun wasSuccessful(evaluatedPath: List<PathEvaluation>): Pair<Boolean, TypedInstance?> {
      val lastEdge = evaluatedPath.last()
      val success = lastEdge is EvaluatedEdge && lastEdge.wasSuccessful
      val resultValue = if (success) {
         selectResultValue(evaluatedPath)
      } else {
         null
      }
      return success to resultValue
   }


   private fun selectResultValue(evaluatedPath: List<PathEvaluation>): TypedInstance? {
      // If the last node in the evaluated path is the type we're after, use that.
      val lastEdgeResult = evaluatedPath.last().resultValue
      if (lastEdgeResult != null && targetType.matches(lastEdgeResult.type)) {
         return lastEdgeResult
      }

      if (lastEdgeResult != null && lastEdgeResult.type.isCalculated && targetType.matches(lastEdgeResult.type)) {
         return lastEdgeResult
      }

      // Handles the case where the target type is an alias for a collection type.
      if (lastEdgeResult != null &&
         targetType.isCollection &&
         targetType.isTypeAlias &&
         targetType.typeParameters.isNotEmpty() &&
         lastEdgeResult.type.matches(targetType.typeParameters.first())
      ) {
         return lastEdgeResult
      }

      // Note - in the old code, this called queryContext..getFactOrNull(targetType, FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT)
      // But, I want to understand why that's nessecary.
      // Investigate if we hit this point
      error("Lookup of results via query context no longer supported - return the search result via the evaluated edge")

      // Why is there a fact in the context that's not present on the last edge result?
      // This will produce incorrect behaviour, where our search was successful,
      // but because the type isn't distinct in the query context, we return null.
//      log().warn("Not sure why this is being called.  Document better, or method will be removed")
//      return queryContext.getFactOrNull(targetType, FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT)
   }

   private fun findPath(
      facts: Collection<TypedInstance>,
      excludedOperations: Set<QualifiedName>,
      excludedEdges: List<EvaluatableEdge>,
      excludedServices: Set<QualifiedName>,
      previouslyEvaluatedPaths: EvaluatedPathSet
   ): WeightedNode<Relationship, Element, Double>? {
      // logTimeTo eats up significant time, so commented out.
      //val graph = logTimeTo(graphBuilderTimes) {
      //
      //   graphBuilder.build(facts, excludedOperations, excludedEdges, excludedServices)
      // }
//      val graphBuildResult = graphBuilder.build(facts, excludedOperations, excludedEdges, excludedServices)
      val graphBuildResult = graphBuilder.build(facts, excludedOperations, excludedEdges, excludedServices)
      val result = findPath(graphBuildResult.graph, previouslyEvaluatedPaths)
      graphBuilder.prune(graphBuildResult)
      return result
   }

   private fun findPath(
      graph: HipsterDirectedGraph<Element, Relationship>,
      evaluatedEdges: EvaluatedPathSet
   ): WeightedNode<Relationship, Element, Double>? {

      // Construct a specialised search problem, which allows us to supply a custom cost function.
      // The cost function applies a higher 'cost' to the nodes transitions that have previously been attempted.
      // (In earlier versions, we simply remvoed edges after failed attempts)
      // This means that transitions that have been tried in a path become less favoured (but still evaluatable)
      // than transitions that haven't been tried.
      val problem = ProblemBuilder.create()
         .initialState(startFact)
         .defineProblemWithExplicitActions()
         .useTransitionFunction { state ->
            graph.outgoingEdgesOf(state).map { edge ->
               Transition.create(state, edge.edgeValue, edge.vertex2)
            }
         }
         .useCostFunction { transition ->
            evaluatedEdges.visitedCountAsCost(transition.fromState, transition.action, transition.state)

         }
         .build()


      val executionPath = logTimeTo(graphSearchTimes) {
         Hipster.createAStar(problem).search(targetFact).goalNode
      }


      log().debug("Generated path with hash ${executionPath.pathHashExcludingWeights()}")
      return if (executionPath.state() != targetFact) {
         null
      } else {
         executionPath
      }
   }

   private fun <R> logTimeTo(timeCollection: MutableList<Long>, operation: () -> R): R {
      val stopwatch = Stopwatch.createStarted()
      val result = operation()
      timeCollection.add(stopwatch.elapsed(TimeUnit.MILLISECONDS))
      return result
   }

}

private fun List<PathEvaluation>.lastEvaluatedEdge(): EvaluatedEdge? {
   return this.last() as? EvaluatedEdge
}
typealias PathEvaluator = (WeightedNode<Relationship, Element, Double>) -> List<PathEvaluation>

data class SearchResult(val typedInstance: TypedInstance?, val path: WeightedNode<Relationship, Element, Double>?) {
   companion object {
      fun noResult(path: WeightedNode<Relationship, Element, Double>?) = SearchResult(null, path)
      fun noPath() = SearchResult(null, null)
   }
}

/**
 * Contains a set of paths that have already been evaluated.
 * The path is hashed excluding the weight of each visited node, as the
 * weight may change as a result of previous visits - however the path itself is still
 * the same path.
 */
class EvaluatedPathSet {
   private val paths: MutableMap<Int, WeightedNode<Relationship, Element, Double>> = mutableMapOf()
   private val transitionCount: MutableMap<HashableTransition, Int> = mutableMapOf()

   fun addPath(path: WeightedNode<Relationship, Element, Double>): Int {
      val hash = path.pathHashExcludingWeights()
      paths[hash] = path

      updateTransitionCount(path)
      return hash
   }

   /**
    * Counts the number of times a specific transition has appeared in the evaluated paths
    */
   private fun updateTransitionCount(node: WeightedNode<Relationship, Element, Double>) {
      node.path()
         .filter { it.previousNode() != null }
         .map { HashableTransition(it.previousNode().state(), it.action(), it.state()) }
         .forEach { transition ->
            transitionCount.compute(transition) { _, currentCount ->
               currentCount?.plus(1) ?: 1
            }
         }
   }

   fun containsPath(path: WeightedNode<Relationship, Element, Double>): Boolean {
      val hash = path.pathHashExcludingWeights()
      return paths.containsKey(hash)
   }

   /**
    * Uses the number of times a specific transition has been used as a cost for evalation.
    * This appraoch ensures that if a transition has been evaluated previously, it is less favoured
    * from another transition.
    * In future, we can tweak this weighting based on action and the outcome of the evaluation
    */
   fun visitedCountAsCost(fromState: Element, action: Relationship, toState: Element): Double {
      val transition = HashableTransition(fromState, action, toState)
      val travsersedCount = transitionCount.getOrDefault(transition, 0)
      // Add one, as this visit, if performed, will be previous number of visits + 1.
      return (travsersedCount + 1) * 1.0
   }

   /**
    * Models a transition of [from]-[relationship]->[to] which can be consistently hashed.
    */
   data class HashableTransition(
      val from: Element,
      val relationship: Relationship,
      val to: Element
   )
}

fun WeightedNode<Relationship, Element, Double>.hashExcludingWeight(): Int {
   return setOf(this.action()?.hashCode() ?: 0, this.state()?.hashCode() ?: 0).hashCode()
}

fun WeightedNode<Relationship, Element, Double>.pathHashExcludingWeights(): Int {
   return this.path().map { it.hashExcludingWeight() }.hashCode()
}
