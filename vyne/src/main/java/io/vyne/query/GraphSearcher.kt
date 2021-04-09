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
import io.vyne.query.graph.*
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

   val searchDescription: String by lazy {
      "Search ${this.startFact.label()} to ${this.targetFact.label()}"
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

      var searchCount = 0
      tailrec fun buildNextPath(): WeightedNode<Relationship, Element, Double>? {
         log().trace("$searchDescription: Attempting to build search path $searchCount")
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
               log().info("The proposed path with id ${proposedPath.pathHashExcludingWeights()} has already been evaluated, so will not be tried again.")
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

      var nextPath = buildNextPath()
      while (nextPath != null) {
         val nextPathId = nextPath.pathHashExcludingWeights()
         evaluatedPaths.addProposedPath(nextPath)

         log().info("$searchDescription - attempting path $nextPathId")
         if (log().isTraceEnabled) {
            log().trace("$searchDescription - attempting path $nextPathId: \n${nextPath.pathDescription()}")
         }

         searchCount++
         if (searchCount > MAX_SEARCH_COUNT) {
            log().error("Search iterations exceeded max count. Stopping, lest we search forever in vein")
            return noResult(nextPath)
         }
         val evaluatedPath = evaluator(nextPath)
         evaluatedPaths.addEvaluatedPath(evaluatedPath)
         val (pathEvaluatedSuccessfully, resultValue, errorMessage) = wasSuccessful(evaluatedPath)
         val resultSatisfiesConstraints =
            pathEvaluatedSuccessfully && invocationConstraints.typedInstanceValidPredicate.isValid(resultValue)
         if (!pathEvaluatedSuccessfully) {
            log().info("$searchDescription - path $nextPathId failed - last error was $errorMessage")
         }

         if (pathEvaluatedSuccessfully && resultSatisfiesConstraints) {
            log().info("$searchDescription - path $nextPathId succeeded with value $resultValue")
            return SearchResult(resultValue, nextPath)
         } else  {
            if (pathEvaluatedSuccessfully && !resultSatisfiesConstraints) {
               log().info("$searchDescription - path $nextPathId executed successfully, but result of $resultValue does not satisfy constraint defined by ${invocationConstraints.typedInstanceValidPredicate::class.simpleName}.  Will continue searching")
            } else {
               log().info("$searchDescription - path $nextPathId did not complete successfully, will continue searching")
            }
            appendIgnorableEdges(evaluatedPath, excludedEdges)
            nextPath = buildNextPath()
         }
      }
      // There were no search paths to evaluate.  Just exit
      //log().info("Failed to find path from ${startFact.label()} to ${targetFact.label()} after $searchCount searches")
      log().trace("$searchDescription ended - no more paths to evaluate")
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

   private fun wasSuccessful(evaluatedPath: List<PathEvaluation>): Triple<Boolean, TypedInstance?, String?> {
      val lastEdge = evaluatedPath.last()
      val success = lastEdge is EvaluatedEdge && lastEdge.wasSuccessful
      val resultValue = if (success) {
         selectResultValue(evaluatedPath)
      } else {
         null
      }
      val errorMessage = if (!success) {
         val evaluatedEdge = lastEdge as EvaluatedEdge
         evaluatedEdge.error
      } else {
         null
      }
      return Triple(success, resultValue, errorMessage)
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

         val problem = ProblemBuilder.create()
            .initialState(startFact)
            .defineProblemWithExplicitActions()
            .useTransitionFunction { state ->
                  graph.outgoingEdgesOf(state).map { edge ->
                     Transition.create(state, edge.edgeValue, edge.vertex2)
                  }
            }
            .useCostFunction { transition ->
               evaluatedEdges.calculateTransitionCost(transition.fromState, transition.action, transition.state)

            }
            .build()


         val executionPath = logTimeTo(graphSearchTimes) {
            Hipster
               .createDijkstra(problem)
               .search(targetFact).goalNode
         }

         log().debug("Generated path with hash ${executionPath.pathHashExcludingWeights()}")
         return if (executionPath.state() != targetFact) {
            null
         } else {
            executionPath
         }
      // Construct a specialised search problem, which allows us to supply a custom cost function.
      // The cost function applies a higher 'cost' to the nodes transitions that have previously been attempted.
      // (In earlier versions, we simply remvoed edges after failed attempts)
      // This means that transitions that have been tried in a path become less favoured (but still evaluatable)
      // than transitions that haven't been tried.




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
