package io.vyne.query

import io.vyne.query.BaseSearchPenaltyProviderImpl.Companion.findFirstIsParameterOnEdgeForFailedOperation
import io.vyne.query.graph.EvaluatableEdge
import io.vyne.query.graph.EvaluatedEdge
import io.vyne.query.graph.PathEvaluation
import io.vyne.schemas.Relationship
import io.vyne.utils.log
import kotlin.math.min

/**
 * After completing an unsuccessful search, we want to adapt the graph where
 * possible to try to find an alternative route, by excluding edges which were
 * troublesome.
 *
 * This is an experiment.
 *
 * MP 4-Feb:  This approach has been phased out in favour of EvluatedPathCostCalculator,
 * which allows edges to stay considered, but are weighted more heavily if they perform badly.
 *
 */
class PathExclusionCalculator {

   fun findEdgesToExclude(
      evaluatedPath: List<PathEvaluation>,
      invocationConstraints: InvocationConstraints
   ): Set<EvaluatableEdge> {
      return listOfNotNull(findFirstIsParameterOnEdgeForFailedOperation(evaluatedPath)).map { it.edge }.toSet()
      // These are a bunch of specific use cases that we've found that are useful to exclude
      val spec = invocationConstraints.typedInstanceValidPredicate

      return listOfNotNull(
         operationThrewError(evaluatedPath, spec)

         // 1-Feb-21: Removed in favour of using transition cost for previously visited nodes.
//         instanceWhichProvidedInvalidMember(evaluatedPath, spec),

         // 1-Feb-21: Removed in favour of using transition cost for previously visited nodes.
//         instanceWhichProviedInvalidInheritedMember(evaluatedPath, spec),

         // 1-Feb-21: Removed in favour of using transition cost for previously visited nodes.
//         operation(evaluatedPath,spec)

//          Excluded prior to 1-Feb-21.
//         operationReturnedResultWhichFailsPredicateTest(evaluatedPath, spec),
//         operationReturnedResultWithAttributeWhichFailsPredicateTest(evaluatedPath, spec)

      ).flatten().toSet()
   }

   private fun operation(
      evaluatedPath: List<PathEvaluation>,
      predicate: TypedInstanceValidPredicate
   ): List<EvaluatableEdge> {
      // This is a risky approach, simply excluding operations.
      // However, we found that the number of permutations that the graph can take to
      // evaluate an operation can be pretty tricky to predict in advance, and brtittle
      // Therefore, we're currently excluding "the nearest" operation, so that the
      // graph can try again.
      val distanceToSearch = min(10, evaluatedPath.size - 1)
      val edgeToExclude = (0..distanceToSearch).asSequence()
         .mapNotNull { index ->
            val evaluation = evaluatedPath.fromEnd(index)
            // This was an operation
            if (evaluation is EvaluatedEdge && evaluation.edge.relationship == Relationship.PROVIDES) {
               evaluation
            } else {
               null
            }
         }
         .firstOrNull()
      return if (edgeToExclude != null) {
         log().info("Excluding operation from current node search:  ${edgeToExclude.description}")
         listOf(edgeToExclude.edge)
      } else {
         emptyList()
      }
   }

   private fun operationThrewError(
      evaluatedPath: List<PathEvaluation>,
      predicate: TypedInstanceValidPredicate
   ): List<EvaluatableEdge> {
      val pathEndedWithOperationThatFailed = evaluatedPath.endsWith(
         Relationship.PROVIDES
      )
      if (!pathEndedWithOperationThatFailed) {
         return emptyList()
      }
      val operationEvaluation = getAsOperationEvaluation(evaluatedPath.fromEnd(0)) ?: return emptyList()
      return if (!operationEvaluation.wasSuccessful) {
         listOf(operationEvaluation.edge)
      } else {
         emptyList()
      }
   }

   private fun operationReturnedResultWithAttributeWhichFailsPredicateTest(
      evaluatedPath: List<PathEvaluation>,
      predicate: TypedInstanceValidPredicate
   ): List<EvaluatableEdge> {
      val trimmedEvaluatedPath = evaluatedPath.excluding(Relationship.EXTENDS_TYPE)

      // These are the paths that can be walked when identifying
      // an operation that failed
      val candidatePaths = listOf(
         listOf(
            Relationship.PROVIDES,
            Relationship.INSTANCE_HAS_ATTRIBUTE,
            Relationship.IS_ATTRIBUTE_OF,
            Relationship.IS_INSTANCE_OF
         ),
         listOf(
            Relationship.PROVIDES,
            Relationship.IS_ATTRIBUTE_OF,
            Relationship.HAS_ATTRIBUTE,
            Relationship.IS_TYPE_OF
         ),
         listOf(
            Relationship.PROVIDES,
            Relationship.IS_INSTANCE_OF,
            Relationship.HAS_ATTRIBUTE,
            Relationship.IS_TYPE_OF
         )

      )

      val matchingCandidatePath = candidatePaths.firstOrNull { trimmedEvaluatedPath.endsWith(it) }
      if (matchingCandidatePath == null) {
         // None of our candidate paths matched
         return emptyList()
      }
      val finalResultFailedPredicate = !predicate.isValid(trimmedEvaluatedPath.last().resultValue)

      if (!finalResultFailedPredicate) {
         return emptyList()
      }
      val nodeIndexToRemove = matchingCandidatePath.size - 1
      val operationEvaluation =
         getAsOperationEvaluation(trimmedEvaluatedPath.fromEnd(nodeIndexToRemove)) ?: return emptyList()
      if (operationEvaluation.edge.relationship != Relationship.PROVIDES) {
         log().error("We should be excluding a PROVIDES relationship")
      }
      return listOf(operationEvaluation.edge)
   }

   private fun getAsOperationEvaluation(pathEvaluation: PathEvaluation): EvaluatedEdge? {
      if (pathEvaluation !is EvaluatedEdge) {
         log().error("Expected to find an evaluated edge, but found a ${pathEvaluation::class.simpleName}.  This is a bug")
         return null
      }
      if (pathEvaluation.edge.relationship != Relationship.PROVIDES) {
         log().error("Expected to find an edge of type Operation -[Provides]-> ??? but found ${pathEvaluation.edge.description}.  This is a bug")
         return null
      }
      return pathEvaluation
   }

   private fun operationReturnedResultWhichFailsPredicateTest(
      evaluatedPath: List<PathEvaluation>,
      predicate: TypedInstanceValidPredicate
   ): List<EvaluatableEdge> {
      val pathEndedByEvaluatingResultFromOperation = evaluatedPath.endsWith(
         Relationship.PROVIDES,
         Relationship.INSTANCE_HAS_ATTRIBUTE
      )
      if (!pathEndedByEvaluatingResultFromOperation) {
         return emptyList()
      }
      val operationEvaluation = getAsOperationEvaluation(evaluatedPath.fromEnd(1)) ?: return emptyList()
      if (operationEvaluation.edge.relationship != Relationship.PROVIDES) {
         log().error("We should be excluding a PROVIDES relationship")
      }
      val isValid = predicate.isValid(operationEvaluation.resultValue)
      return if (isValid) {
         return emptyList()
      } else {
         listOf(operationEvaluation.edge)
      }
   }

   private fun instanceWhichProviedInvalidInheritedMember(
      evaluatedPath: List<PathEvaluation>,
      spec: TypedInstanceValidPredicate
   ): List<EvaluatableEdge> {
      // This path deals sepcifically with an inhertited / formatted type
      // that fails the predicate test.
      val pathEndedBySelectingValueFromInstance = evaluatedPath.endsWith(
         Relationship.INSTANCE_HAS_ATTRIBUTE,
         Relationship.IS_ATTRIBUTE_OF,
         Relationship.IS_INSTANCE_OF,
         Relationship.EXTENDS_TYPE,
         Relationship.CAN_POPULATE
      )
      val selectedInstanceFailedTest = !spec.isValid(evaluatedPath.last().resultValue)
      return if (pathEndedBySelectingValueFromInstance && selectedInstanceFailedTest) {
         val evaluation = evaluatedPath.fromEnd(4)
         // evaluation should be TypedInstance -[Instance has attribute]-> Provied Instance Member
         if (evaluation is EvaluatedEdge) {
            listOf(evaluation.edge)
         } else {
            log().error("Expected to find an evaluated edge, but found a ${evaluation::class.simpleName}.  This is a bug")
            emptyList()
         }
      } else {
         emptyList()
      }
   }

   private fun instanceWhichProvidedInvalidMember(
      evaluatedPath: List<PathEvaluation>,
      spec: TypedInstanceValidPredicate
   ): List<EvaluatableEdge> {
      // Look for a recent (ie., near the end of the path) instance
      // which provided a value that failed our search criteria spec
      val pathWithRelevantNodes = evaluatedPath.excluding(Relationship.EXTENDS_TYPE)
      val pathEndedBySelectingValueFromInstance = pathWithRelevantNodes.endsWith(
         Relationship.INSTANCE_HAS_ATTRIBUTE,
         Relationship.IS_ATTRIBUTE_OF,
         Relationship.IS_INSTANCE_OF
      )
      val selectedInstanceFailedTest = !spec.isValid(evaluatedPath.last().resultValue)
      return if (pathEndedBySelectingValueFromInstance && selectedInstanceFailedTest) {
         val evaluation = pathWithRelevantNodes.fromEnd(2)
         // evaluation should be TypedInstance -[Instance has attribute]-> Provied Instance Member
         if (evaluation is EvaluatedEdge) {
            listOf(evaluation.edge)
         } else {
            log().error("Expected to find an evaluated edge, but found a ${evaluation::class.simpleName}.  This is a bug")
            emptyList()
         }
      } else {
         emptyList()
      }
   }
}

private fun List<PathEvaluation>.excluding(relationship: Relationship): List<PathEvaluation> {
   return this.filter {
      !(it is EvaluatedEdge && it.edge.relationship == relationship)
   }
}

private fun <T> List<T>.fromEnd(count: Int): T {
   val index = this.lastIndex - count
   return this[index]
}

private fun List<PathEvaluation>.endsWith(relationships: List<Relationship>): Boolean {
   if (this.size < relationships.size) {
      return false
   }
   return relationships
      .asSequence()
      .mapIndexed { index, relationship ->
         val offsetIndex = this.size - relationships.size + index
         val evaluation = this[offsetIndex]
         evaluation is EvaluatedEdge && evaluation.edge.relationship == relationship
      }
      .all { it }

}

private fun List<PathEvaluation>.endsWith(vararg relationships: Relationship): Boolean {
   return this.endsWith(relationships.toList())
}
