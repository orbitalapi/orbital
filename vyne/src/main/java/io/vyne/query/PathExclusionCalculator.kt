package io.vyne.query

import io.vyne.models.TypedNull
import io.vyne.query.graph.EvaluatableEdge
import io.vyne.query.graph.EvaluatedEdge
import io.vyne.query.graph.PathEvaluation
import io.vyne.schemas.Relationship
import io.vyne.utils.log

/**
 * After completing an unsuccessful search, we want to adapt the graph where
 * possible to try to find an alternative route, by excluding edges which were
 * troublesome.
 *
 * This is an experiment
 *
 */
class PathExclusionCalculator {

   fun findEdgesToExclude(evaluatedPath: List<PathEvaluation>, spec: TypedInstanceValidPredicate): List<EvaluatableEdge> {
      // These are a bunch of specific use cases that we've found that are useful to exclude
      return listOfNotNull(
         instanceWhichProvidedInvalidMember(evaluatedPath, spec),
         operationReturnedResultWhichFailsPredicateTest(evaluatedPath, spec),
         operationReturnedResultWithAttributeWhichFailsPredicateTest(evaluatedPath, spec)

      ).flatten()
   }

   private fun operationReturnedResultWithAttributeWhichFailsPredicateTest(evaluatedPath: List<PathEvaluation>, predicate: TypedInstanceValidPredicate): List<EvaluatableEdge> {
      val pathEndedByEvaluatingAttriubteOnResultFromOperation = evaluatedPath.endsWith(
         Relationship.PROVIDES,
         Relationship.INSTANCE_HAS_ATTRIBUTE,
         Relationship.IS_ATTRIBUTE_OF,
         Relationship.IS_INSTANCE_OF
      )
      if (!pathEndedByEvaluatingAttriubteOnResultFromOperation) {
         return emptyList()
      }
      val finalResultFailedPredicate = !predicate.isValid(evaluatedPath.last().resultValue)

      if (!finalResultFailedPredicate) {
         return emptyList()
      }
      val operationEvaluation = evaluatedPath.fromEnd(3)
      if (operationEvaluation !is EvaluatedEdge) {
         log().error("Expected to find an evaluated edge, but found a ${operationEvaluation::class.simpleName}.  This is a bug")
         return emptyList()
      }
      if (operationEvaluation.edge.relationship != Relationship.PROVIDES) {
         log().error("Expected to find an edge of type Operation -[Provides]-> ??? but found ${operationEvaluation.edge.description}.  This is a bug")
         return emptyList()
      }
      return listOf(operationEvaluation.edge)


   }

   private fun operationReturnedResultWhichFailsPredicateTest(evaluatedPath: List<PathEvaluation>, predicate: TypedInstanceValidPredicate): List<EvaluatableEdge> {
      val pathEndedByEvaluatingResultFromOperation = evaluatedPath.endsWith(
         Relationship.PROVIDES,
         Relationship.INSTANCE_HAS_ATTRIBUTE
      )
      if (!pathEndedByEvaluatingResultFromOperation) {
         return emptyList()
      }
      val operationEvaluation = evaluatedPath.fromEnd(1)
      return excludeOperationIfResponseFailsPredicate(operationEvaluation, predicate)
   }

   private fun excludeOperationIfResponseFailsPredicate(operationEvaluation: PathEvaluation, predicate: TypedInstanceValidPredicate): List<EvaluatableEdge> {
      if (operationEvaluation !is EvaluatedEdge) {
         log().error("Expected to find an evaluated edge, but found a ${operationEvaluation::class.simpleName}.  This is a bug")
         return emptyList()
      }
      if (operationEvaluation.edge.relationship != Relationship.PROVIDES) {
         log().error("Expected to find an edge of type Operation -[Provides]-> ??? but found ${operationEvaluation.edge.description}.  This is a bug")
         return emptyList()
      }
      val isValid = predicate.isValid(operationEvaluation.resultValue)
      return if (isValid) {
         return emptyList()
      } else {
         listOf(operationEvaluation.edge)
      }
   }

   private fun instanceWhichProvidedInvalidMember(evaluatedPath: List<PathEvaluation>, spec: TypedInstanceValidPredicate): List<EvaluatableEdge> {
      // Look for a recent (ie., near the end of the path) instance
      // which provided a value that failed our search criteria spec
      val pathEndedBySelectingValueFromInstance = evaluatedPath.endsWith(
         Relationship.INSTANCE_HAS_ATTRIBUTE,
         Relationship.IS_ATTRIBUTE_OF,
         Relationship.IS_INSTANCE_OF
      )
      val selectedInstanceFailedTest = !spec.isValid(evaluatedPath.last().resultValue)
      return if (pathEndedBySelectingValueFromInstance && selectedInstanceFailedTest) {
         val evaluation = evaluatedPath.fromEnd(2)
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

private fun <T> List<T>.fromEnd(count: Int): T {
   val index = this.lastIndex - count
   return this[index]
}

private fun List<PathEvaluation>.endsWith(vararg relationships: Relationship): Boolean {
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
