package io.vyne.query

import es.usc.citius.hipster.model.impl.WeightedNode
import io.vyne.query.graph.Element
import io.vyne.query.graph.ElementType
import io.vyne.query.graph.EvaluatedEdge
import io.vyne.query.graph.PathEvaluation
import io.vyne.query.graph.pathHashExcludingWeights
import io.vyne.schemas.Relationship


/**
 * Contains a set of paths that have already been evaluated.
 * The path is hashed excluding the weight of each visited node, as the
 * weight may change as a result of previous visits - however the path itself is still
 * the same path.
 */
class EvaluatedPathSet {
   private val proposedPaths: MutableMap<Int, WeightedNode<Relationship, Element, Double>> = mutableMapOf()
   private val evaluatedPaths: MutableList<List<PathEvaluation>> = mutableListOf()
   private val evaluatedOperations: MutableList<EvaluatedEdge> = mutableListOf()
   private val penalizedEdges: MutableList<PenalizedEdge> = mutableListOf()
   private val transitionCount: MutableMap<HashableTransition, Int> = mutableMapOf()


   companion object {
      private val PENALTY_COST = 100.0
   }

   fun addProposedPath(path: WeightedNode<Relationship, Element, Double>): Int {
      val hash = path.pathHashExcludingWeights()
      proposedPaths[hash] = path

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
      return proposedPaths.containsKey(hash)
   }

   /**
    * Uses the number of times a specific transition has been used as a cost for evalation.
    * This appraoch ensures that if a transition has been evaluated previously, it is less favoured
    * from another transition.
    * In future, we can tweak this weighting based on action and the outcome of the evaluation
    */
   private fun visitedCountAsCost(fromState: Element, action: Relationship, toState: Element): Double {
      val transition = HashableTransition(fromState, action, toState)
      val travsersedCount = transitionCount.getOrDefault(transition, 0)
      // Add one, as this visit, if performed, will be previous number of visits + 1.
      return (travsersedCount + 1) * 1.0
   }

   /**
    * Looks to see if the transition has been previously tagged as penalized, and if so,
    * returns a higher weighting, because it's been a bad transition. BAD BAD TRANSITION!
    * GO TO YOUR ROOM.
    */
   fun calculateTransitionCost(fromState: Element, action: Relationship, toState: Element): Double {
      // If we haven't done anything before, everything is equal
      if (evaluatedPaths.isEmpty()) {
         return 1.0
      }


      val penalizedEdge: PenalizedEdge? = this.penalizedEdges.filter {
         it.matches(fromState, action, toState)
      }.maxBy { it.penalty }
      return if (penalizedEdge != null) {
         penalizedEdge.penalty
      } else {
         // If the edge hasn't been explitily penalized, we still apply a heavier
         // weighting to paths we've walked before.
         // This is to ensure that when multiple valid paths are present without having
         // incurred an explicitly defined penalty (ie., for a service that returned a bad value),
         // that new paths can still emerge.
         // For example - For a service that returned a value with two matching
         // fields, this ensures that the path selector to both fields gets given a chance
         // to be evaluated
         visitedCountAsCost(fromState, action, toState)
      }

      // Design note:
      // TEST: // This is tested using VyneTest.when one operation failed but another path is present with different inputs then the different path is tried
      // We've tried a bunch of different things here, including looking to penalize the cost of
      // operations that have failed.
      // The important behaviour we're looking for is to penalize the right point in the path
      // such that another, better path can be found.
      // We discovered that by penalizing operations, that excludes them from being invoked again
      // with different inputs.  We tried a bunch of different strategies, two of which I'm documenting here for the next time
      // we revisit this problem.
      // First we tried using multiple combinations of heavier (positive weighted) penatlies for operations that we'd visited.
      // eg: Service calls that we've tried successfully cost 0
      // Service calls that we've tried which failed cost 100
      // Service calls that we haven't tried cost 10
      // However, this could never work, as we're specifically trying to get the graph to provide a path that includes
      // all the bits of the previously failed path, plus some new work to discover alternative inputs.  Therefore, whatever
      // weight we applied to an operation - the path without the new work was always cheaper.
      // The next thing we tried was using using negative weights
      // on operations that hadn't been used and penalizing those that had been used (and penalizing those that had
      // been used and failed even harder).  This approach didn't work because:
      // a) Djikstra doesn't permit negative weights on edges
      // b) When trying a BellmanFord search, we hit errors around "Negative weight cycles" - which is either a bug in the
      // graph library, or we actually have circular references in our graph.
      // Finally we arrived on penalizing the edge that supplied the bad input.  This worked.
      // I've split the implementation up to allow us to provide other weighting strategies here too in future.


   }

   fun addEvaluatedPath(evaluatedPath: List<PathEvaluation>) {
      this.evaluatedPaths.add(evaluatedPath)

      this.penalizedEdges.addAll(findEdgesToPenalize(evaluatedPath))
      val operations = evaluatedPath.filterIsInstance<EvaluatedEdge>()
         .filter { it.edge.vertex1.elementType == ElementType.OPERATION && it.edge.relationship == Relationship.PROVIDES }
      this.evaluatedOperations.addAll(operations)
   }

   private fun findEdgesToPenalize(evaluatedPath: List<PathEvaluation>): List<PenalizedEdge> {
      return listOf(
         findPenaltiesForFailedOperation(evaluatedPath)
      ).flatten()
   }



   /**
    * This looks for a failed operation call, then backtracks to find the provider
    * of the inputs.
    *
    * We penalize the input, as well as the operation, so that the graph will try to find
    * another way to supply different inputs to the operation
    */
   private fun findPenaltiesForFailedOperation(evaluatedPath: List<PathEvaluation>): List<PenalizedEdge> {
      val indexOfFailedOperation = evaluatedPath.indexOfFirst {
         it is EvaluatedEdge &&
            !it.wasSuccessful &&
            it.edge.vertex1.elementType == ElementType.OPERATION
      }
      if (indexOfFailedOperation == -1) return emptyList()
      val failedOperationEdge = evaluatedPath[indexOfFailedOperation] as EvaluatedEdge
      val failedOperationPenalty = PenalizedEdge(
         failedOperationEdge,
         evaluatedPath,
         "Operation failed with error ${failedOperationEdge.error}",
         penalty = PENALTY_COST
      )
      // This finds the PROVIDED_INSTANCE_MEMBER edge which was used to populate the input
      // parameter onto the operation.
      val indexOfParameterInput = (indexOfFailedOperation downTo 0)
         .firstOrNull { index ->
            val edge = evaluatedPath[index]
            edge is EvaluatedEdge &&
               edge.edge.vertex1.elementType == ElementType.PROVIDED_INSTANCE_MEMBER
         }
      val failedInputPenalty = if (indexOfParameterInput != null) {
         PenalizedEdge(
            evaluatedPath[indexOfParameterInput] as EvaluatedEdge,
            evaluatedPath,
            "Input into a failed operation",
            penalty = PENALTY_COST
         )
      } else null
      return listOfNotNull(
         failedOperationPenalty,
         failedInputPenalty
      )
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

data class PenalizedEdge(
   val evaluatedEdge: EvaluatedEdge,
   val originatingPath: List<PathEvaluation>,
   // This reason isn't used except by engineers trying to work out
   // what the fuck is going on.
   val reason: String,
   val penalty: Double
) {
   fun matches(fromState: Element, action: Relationship, toState: Element): Boolean {
      return this.evaluatedEdge.edge.vertex1 == fromState
         && this.evaluatedEdge.edge.relationship == action
         && this.evaluatedEdge.edge.vertex2 == toState
   }
}


