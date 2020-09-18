package io.vyne.query

import com.google.common.base.Stopwatch
import es.usc.citius.hipster.algorithm.Hipster
import es.usc.citius.hipster.graph.GraphSearchProblem
import es.usc.citius.hipster.graph.HipsterDirectedGraph
import es.usc.citius.hipster.model.impl.WeightedNode
import io.vyne.models.TypedInstance
import io.vyne.query.graph.*
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Relationship
import io.vyne.schemas.Type
import io.vyne.utils.log
import java.util.concurrent.TimeUnit

// This class is not optimized.  Need to investigate how to speed it up.
class GraphSearcher(private val startFact: Element, private val targetFact: Element, private val targetType: Type, private val graphBuilder: VyneGraphBuilder, private val buildSpec: TypedInstanceValidPredicate) {

   private val graphBuilderTimes = mutableListOf<Long>()
   private val graphSearchTimes = mutableListOf<Long>()
   private val pathExclusionCalculator = PathExclusionCalculator()

   companion object {
      const val MAX_SEARCH_COUNT = 100
   }

   fun search(knownFacts: Set<TypedInstance>, evaluator: PathEvaluator): TypedInstance? {
      // TODO : EEEK!  We should be adding the instances, not the types.
      // This will cause problems when we have multiple facts of the same type,
      // as one may result in a happy path, and the other might not.
//      val factTypes = knownFacts.map { it.type }.toSet()

      val excludedOperations = mutableSetOf<QualifiedName>()
      val excludedInstance = mutableSetOf<TypedInstance>()
      val excludedEdges = mutableListOf<EvaluatableEdge>()
      fun buildNextPath(): WeightedNode<Relationship, Element, Double>? {
         val facts = knownFacts.filterNot { excludedInstance.contains(it) }
         // Note: I think we can migrate to using exclusively excludedEdges (Not using excludedOperations
         // and excludedInstances)..as it should be a more powerful abstraction
         return findPath(facts, excludedOperations, excludedEdges)
      }

      var searchCount = 0
      var nextPath = buildNextPath()
      while (nextPath != null) {
         searchCount++
         if (searchCount > MAX_SEARCH_COUNT) {
            log().error("Search iterations exceeded max count. Stopping, lest we search forever in vein")
            return null
         }
         val evaluatedPath = evaluator(nextPath)
         val (pathEvaluatedSuccessfully, resultValue) = wasSuccessful(evaluatedPath)
         if (pathEvaluatedSuccessfully) {
            if (buildSpec.isValid(resultValue)) {
               return resultValue
            } else {

               // Check to see if there are any nodes in the executed path
               // that we want to ignore and try a new search
               if (appendIgnorableEdges(evaluatedPath, excludedEdges)) {
                  nextPath = buildNextPath()
               } else {
                  log().info("Search found an instance which failed the provided spec, and couldn't find anything to exclude - giving up")
                  return null
               }
            }
         } else {
            val lastStep = evaluatedPath.lastEvaluatedEdge()
            if (lastStep == null) {
               // Giving up.  However, perhaps there are other opportunities here later.
               return null
            }
            // Check to see if there are any nodes in the executed path
            // that we want to ignore and try a new search
            if (appendIgnorableEdges(evaluatedPath, excludedEdges)) {
               nextPath = buildNextPath()
            } else {
               // TODO : Migrate this into the PathExclusionCalcaultor
               if (lastStep.edge.vertex1.elementType == ElementType.OPERATION) {
                  // We tried to call an operation, and it failed.
                  // Remove the operation from the graph, and try searching again to see if there's another path
                  excludedOperations.add(lastStep.edge.vertex1.valueAsQualifiedName())
                  nextPath = buildNextPath()
               } else {
                  // Giving up.  However, perhaps there are other opportunities here later.
                  return null
               }
               log().info("Search found an instance which failed the provided spec, and couldn't find anything to exclude - giving up")
               return null
            }

         }
      }
      // There were no search paths to evaluate.  Just exit
      return null
   }

   private fun appendIgnorableEdges(evaluatedPath: List<PathEvaluation>, excludedEdges: MutableList<EvaluatableEdge>): Boolean {
      val edgesToExclude = pathExclusionCalculator.findEdgesToExclude(evaluatedPath, buildSpec)
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
      if (lastEdgeResult != null && lastEdgeResult.type.matches(targetType)) {
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
         lastEdgeResult.type.matches(targetType.typeParameters.first())) {
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

   private fun findPath(factTypes: Set<Type>, excludedOperations: Set<QualifiedName>): WeightedNode<Relationship, Element, Double>? {
      val graph = logTimeTo(graphBuilderTimes) {
         graphBuilder.build(factTypes, excludedOperations)
      }
      return findPath(graph)
   }

   private fun findPath(facts: List<TypedInstance>, excludedOperations: Set<QualifiedName>, excludedEdges: List<EvaluatableEdge>): WeightedNode<Relationship, Element, Double>? {
      val graph = logTimeTo(graphBuilderTimes) {
         graphBuilder.build(facts, excludedOperations, excludedEdges)
      }
      return findPath(graph)
   }

   private fun findPath(graph: HipsterDirectedGraph<Element, Relationship>): WeightedNode<Relationship, Element, Double>? {
      val problem = GraphSearchProblem
         .startingFrom(startFact).`in`(graph)
         .takeCostsFromEdges()
         .build()
      val executionPath = logTimeTo(graphSearchTimes) {
         Hipster.createAStar(problem).search(targetFact).goalNode
      }

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
